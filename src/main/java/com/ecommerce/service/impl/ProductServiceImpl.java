package com.ecommerce.service.impl;

import com.ecommerce.dto.request.product.CreateProductRequest;
import com.ecommerce.dto.request.product.CreateVariantRequest;
import com.ecommerce.dto.request.product.UpdateProductRequest;
import com.ecommerce.dto.response.product.LowStockVariantResponse;
import com.ecommerce.dto.response.product.ProductDetailResponse;
import com.ecommerce.dto.response.product.ProductImageResponse;
import com.ecommerce.dto.response.product.ProductResponse;
import com.ecommerce.dto.response.product.VariantResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.Shop;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.FlashSaleProductRepository;
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "DELETED");
    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartItemRepository cartItemRepository;
    private final FlashSaleProductRepository flashSaleProductRepository;

    @Override
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest req, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", shopId));
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));

        Product product = new Product();
        product.setShop(shop);
        product.setCategory(category);
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setBasePrice(req.getBasePrice());
        product.setStatus(normalizeStatus(req.getStatus(), "ACTIVE"));
        // logic applyFlashSale cũ đã bị gỡ bỏ

        product.setSlug(generateTemporarySlug(req.getName()));
        product = productRepository.save(product);

        product.setSlug(generateProductSlug(req.getName(), product.getId()));
        replaceImages(product, req.getImageUrls());
        upsertVariants(product, req.getVariants(), false);

        product = productRepository.save(product);
        return mapToDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getShopProducts(Long shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findByShopIdAndStatusNot(shopId, "DELETED", pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPublicProducts() {
        return productRepository.findAllActiveProducts().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if ("DELETED".equals(product.getStatus())) {
            throw new ResourceNotFoundException("Product", id);
        }

        return mapToDetailResponse(product);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long id, UpdateProductRequest req, Long shopId) {
        Product product = productRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if ("DELETED".equals(product.getStatus())) {
            throw new BusinessException("San pham da bi xoa");
        }

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));
            product.setCategory(category);
        }

        if (req.getName() != null && !req.getName().isBlank()) {
            product.setName(req.getName());
            product.setSlug(generateProductSlug(req.getName(), product.getId()));
        }

        if (req.getDescription() != null) {
            product.setDescription(req.getDescription());
        }

        if (req.getBasePrice() != null) {
            product.setBasePrice(req.getBasePrice());
        }

        if (req.getStatus() != null) {
            product.setStatus(normalizeStatus(req.getStatus(), product.getStatus()));
        }

        // Bỏ logic applyFlashSale trực tiếp vào Product
        // Việc đăng ký sản phẩm vào Flash Sale sẽ được thực hiện qua FlashSaleSellerController

        if (req.getImageUrls() != null) {
            replaceImages(product, req.getImageUrls());
        }

        if (req.getVariants() != null) {
            upsertVariants(product, req.getVariants(), true);
        }

        if (req.getStockQuantity() != null) {
            product.setStockQuantity(req.getStockQuantity());
        }

        product = productRepository.save(product);
        return mapToDetailResponse(product);
    }

    @Override
    @Transactional
    public void softDeleteProduct(Long id, Long shopId) {
        Product product = productRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setStatus("DELETED");
        productRepository.save(product);
    }

    @Override
    @Transactional
    public VariantResponse updateVariantStock(Long variantId, int newStock, Long shopId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));

        Product product = variant.getProduct();
        if (!product.getShop().getId().equals(shopId)) {
            throw new BusinessException("Khong co quyen sua ton kho san pham nay");
        }

        variant.setStock(newStock);
        productVariantRepository.save(variant);
        recalculateProductStock(product);
        productRepository.save(product);

        return mapVariant(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockVariantResponse> getLowStockVariants(Long shopId) {
        return productVariantRepository.findByProduct_Shop_IdAndStockLessThan(shopId, LOW_STOCK_THRESHOLD).stream()
                .map(variant -> {
                    LowStockVariantResponse response = new LowStockVariantResponse();
                    response.setVariantId(variant.getId());
                    response.setSku(variant.getSku());
                    response.setAttributes(variant.getAttributes());
                    response.setStock(variant.getStock());
                    response.setProductId(variant.getProduct().getId());
                    response.setProductName(variant.getProduct().getName());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void replaceImages(Product product, List<String> imageUrls) {
        product.getImages().clear();
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(imageUrls.get(i));
            image.setIsPrimary(i == 0);
            product.getImages().add(image);
        }
    }

    private void upsertVariants(Product product, List<CreateVariantRequest> requests, boolean validateRemoval) {
        product.getVariants().clear();

        if (requests == null || requests.isEmpty()) {
            product.setStockQuantity(0);
            return;
        }

        if (validateRemoval) {
            List<Long> existingVariantIds = productVariantRepository.findByProductId(product.getId()).stream()
                    .map(ProductVariant::getId)
                    .collect(Collectors.toList());
            Set<Long> incomingIds = requests.stream()
                    .map(CreateVariantRequest::getId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());

            for (Long existingId : existingVariantIds) {
                if (!incomingIds.contains(existingId)) {
                    validateVariantCanBeRemoved(existingId);
                }
            }
        }

        for (CreateVariantRequest request : requests) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setAttributes(request.getAttributes());
            variant.setPrice(request.getPrice());
            variant.setStock(request.getStock() != null ? request.getStock() : 0);
            variant.setSku(request.getSku());
            product.getVariants().add(variant);
        }

        recalculateProductStock(product);
    }

    private void validateVariantCanBeRemoved(Long variantId) {
        if (cartItemRepository.existsByVariantId(variantId)) {
            throw new BusinessException("Khong the xoa bien the dang duoc su dung trong gio hang");
        }
    }

    private void recalculateProductStock(Product product) {
        int totalStock = product.getVariants().stream()
                .mapToInt(variant -> variant.getStock() != null ? variant.getStock() : 0)
                .sum();
        product.setStockQuantity(totalStock);
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setBasePrice(product.getBasePrice());
        response.setPrimaryImageUrl(resolvePrimaryImage(product));
        response.setStockQuantity(product.getStockQuantity());
        response.setStatus(product.getStatus());
        response.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        response.setRating(product.getRating());
        response.setSoldCount(product.getSoldCount());
        response.setCreatedAt(product.getCreatedAt());

        // Tìm thông tin Flash Sale từ bảng mới
        flashSaleProductRepository.findActiveByProductId(product.getId()).ifPresent(fsp -> {
            response.setFlashSaleActive(true);
            response.setFlashSalePrice(fsp.getFlashSalePrice());
            response.setFlashSaleStartAt(fsp.getFlashSale().getStartTime());
            response.setFlashSaleEndAt(fsp.getFlashSale().getEndTime());
            response.setEffectivePrice(fsp.getFlashSalePrice());
        });

        if (response.getEffectivePrice() == null) {
            response.setEffectivePrice(product.getBasePrice());
            response.setFlashSaleActive(false);
        }

        return response;
    }

    private ProductDetailResponse mapToDetailResponse(Product product) {
        ProductDetailResponse response = new ProductDetailResponse();
        ProductResponse base = mapToResponse(product);

        response.setId(base.getId());
        response.setName(base.getName());
        response.setBasePrice(base.getBasePrice());
        response.setPrimaryImageUrl(base.getPrimaryImageUrl());
        response.setStockQuantity(base.getStockQuantity());
        response.setStatus(base.getStatus());
        response.setCategoryName(base.getCategoryName());
        response.setRating(base.getRating());
        response.setSoldCount(base.getSoldCount());
        response.setCreatedAt(base.getCreatedAt());
        response.setEffectivePrice(base.getEffectivePrice());
        response.setFlashSaleActive(base.getFlashSaleActive());
        response.setFlashSalePrice(base.getFlashSalePrice());
        response.setFlashSaleStartAt(base.getFlashSaleStartAt());
        response.setFlashSaleEndAt(base.getFlashSaleEndAt());

        response.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        response.setDescription(product.getDescription());
        response.setSlug(product.getSlug());
        response.setShopId(product.getShop() != null ? product.getShop().getId() : null);
        response.setShopName(product.getShop() != null ? product.getShop().getName() : null);
        response.setImages(product.getImages().stream()
                .map(image -> {
                    ProductImageResponse imageResponse = new ProductImageResponse();
                    imageResponse.setId(image.getId());
                    imageResponse.setImageUrl(image.getImageUrl());
                    imageResponse.setIsPrimary(image.getIsPrimary());
                    return imageResponse;
                })
                .collect(Collectors.toList()));
        response.setVariants(product.getVariants().stream()
                .map(this::mapVariant)
                .collect(Collectors.toList()));
        return response;
    }

    private VariantResponse mapVariant(ProductVariant variant) {
        VariantResponse response = new VariantResponse();
        response.setId(variant.getId());
        response.setSku(variant.getSku());
        response.setAttributes(variant.getAttributes());
        response.setPrice(variant.getPrice());
        response.setStock(variant.getStock());
        return response;
    }

    private String normalizeStatus(String status, String fallback) {
        String normalized = status == null || status.isBlank()
                ? fallback
                : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BusinessException("Status san pham khong hop le");
        }
        return normalized;
    }

    private String resolvePrimaryImage(Product product) {
        return product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());
    }

    private String generateTemporarySlug(String input) {
        return generateSlug(input) + "-" + System.currentTimeMillis();
    }

    private String generateProductSlug(String input, Long id) {
        return generateSlug(input) + "-" + id;
    }

    private String generateSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
    }
}
