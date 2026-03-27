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
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartItemRepository cartItemRepository;

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
        applyFlashSale(product, req.getFlashSaleEnabled(), req.getFlashSalePrice(), req.getFlashSaleStartAt(), req.getFlashSaleEndAt());
        product.setSlug(generateSlug(req.getName()) + "-" + System.currentTimeMillis());

        product = productRepository.save(product);
        product.setSlug(generateSlug(req.getName()) + "-" + product.getId());

        replaceImages(product, req.getImageUrls());
        upsertVariants(product, req.getVariants(), false);

        product = productRepository.save(product);
        return mapToDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getShopProducts(Long shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findByShopIdAndStatusNot(shopId, "DELETED", pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPublicProducts() {
        return productRepository.findAllActiveProducts().stream().map(this::mapToResponse).collect(Collectors.toList());
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
        if (req.getName() != null) {
            product.setName(req.getName());
            product.setSlug(generateSlug(req.getName()) + "-" + product.getId());
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
        if (req.getFlashSaleEnabled() != null || req.getFlashSalePrice() != null || req.getFlashSaleStartAt() != null || req.getFlashSaleEndAt() != null) {
            applyFlashSale(
                    product,
                    req.getFlashSaleEnabled() != null ? req.getFlashSaleEnabled() : product.getFlashSaleEnabled(),
                    req.getFlashSalePrice() != null ? req.getFlashSalePrice() : product.getFlashSalePrice(),
                    req.getFlashSaleStartAt() != null ? req.getFlashSaleStartAt() : product.getFlashSaleStartAt(),
                    req.getFlashSaleEndAt() != null ? req.getFlashSaleEndAt() : product.getFlashSaleEndAt()
            );
        }

        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            replaceImages(product, req.getImageUrls());
        }
        if (req.getVariants() != null) {
            upsertVariants(product, req.getVariants(), true);
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
        return mapVariant(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockVariantResponse> getLowStockVariants(Long shopId) {
        return productVariantRepository.findByProduct_Shop_IdAndStockLessThan(shopId, 10).stream().map(v -> {
            LowStockVariantResponse res = new LowStockVariantResponse();
            res.setVariantId(v.getId());
            res.setSku(v.getSku());
            res.setAttributes(v.getAttributes());
            res.setStock(v.getStock());
            res.setProductId(v.getProduct().getId());
            res.setProductName(v.getProduct().getName());
            return res;
        }).collect(Collectors.toList());
    }

    private void replaceImages(Product product, List<String> imageUrls) {
        product.getImages().clear();
        productRepository.saveAndFlush(product);
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(imageUrls.get(i));
            image.setIsPrimary(i == 0);
            product.getImages().add(image);
        }
    }

    private void upsertVariants(Product product, List<CreateVariantRequest> requests, boolean validateRemoval) {
        Set<Long> incomingIds = requests.stream()
                .map(CreateVariantRequest::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        if (validateRemoval) {
            for (ProductVariant existing : product.getVariants()) {
                if (!incomingIds.contains(existing.getId())) {
                    validateVariantCanBeRemoved(existing.getId());
                }
            }
        }

        List<ProductVariant> currentVariants = product.getVariants();
        Set<Long> currentIds = currentVariants.stream()
                .map(ProductVariant::getId)
                .collect(Collectors.toSet());

        currentVariants.removeIf(existing -> existing.getId() != null && !incomingIds.contains(existing.getId()));

        for (CreateVariantRequest request : requests) {
            ProductVariant variant;
            if (request.getId() != null && currentIds.contains(request.getId())) {
                variant = currentVariants.stream()
                        .filter(item -> request.getId().equals(item.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", request.getId()));
            } else {
                variant = new ProductVariant();
                variant.setProduct(product);
                currentVariants.add(variant);
            }

            variant.setAttributes(request.getAttributes());
            variant.setPrice(request.getPrice());
            variant.setStock(request.getStock() != null ? request.getStock() : 0);
            variant.setSku(request.getSku());
        }

        recalculateProductStock(product);
    }

    private void validateVariantCanBeRemoved(Long variantId) {
        if (cartItemRepository.existsByVariantId(variantId)) {
            throw new BusinessException("Khong the xoa bien the dang ton tai trong gio hang");
        }
    }

    private void recalculateProductStock(Product product) {
        int totalStock = product.getVariants().stream().mapToInt(ProductVariant::getStock).sum();
        product.setStockQuantity(totalStock);
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse res = new ProductResponse();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setBasePrice(product.getBasePrice());
        res.setPrimaryImageUrl(resolvePrimaryImage(product));
        res.setStockQuantity(product.getStockQuantity());
        res.setStatus(product.getStatus());
        res.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        res.setRating(product.getRating());
        res.setSoldCount(product.getSoldCount());
        res.setCreatedAt(product.getCreatedAt());
        res.setEffectivePrice(resolveEffectivePrice(product));
        res.setFlashSaleActive(isFlashSaleActive(product));
        res.setFlashSalePrice(product.getFlashSalePrice());
        res.setFlashSaleStartAt(product.getFlashSaleStartAt());
        res.setFlashSaleEndAt(product.getFlashSaleEndAt());
        return res;
    }

    private ProductDetailResponse mapToDetailResponse(Product product) {
        ProductDetailResponse res = new ProductDetailResponse();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setBasePrice(product.getBasePrice());
        res.setPrimaryImageUrl(resolvePrimaryImage(product));
        res.setStockQuantity(product.getStockQuantity());
        res.setStatus(product.getStatus());
        res.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        res.setRating(product.getRating());
        res.setSoldCount(product.getSoldCount());
        res.setCreatedAt(product.getCreatedAt());
        res.setEffectivePrice(resolveEffectivePrice(product));
        res.setFlashSaleActive(isFlashSaleActive(product));
        res.setFlashSalePrice(product.getFlashSalePrice());
        res.setFlashSaleStartAt(product.getFlashSaleStartAt());
        res.setFlashSaleEndAt(product.getFlashSaleEndAt());
        res.setDescription(product.getDescription());
        res.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        res.setSlug(product.getSlug());
        res.setShopId(product.getShop().getId());
        res.setShopName(product.getShop().getName());
        res.setImages(product.getImages().stream().map(img -> {
            ProductImageResponse imageResponse = new ProductImageResponse();
            imageResponse.setId(img.getId());
            imageResponse.setImageUrl(img.getImageUrl());
            imageResponse.setIsPrimary(img.getIsPrimary());
            return imageResponse;
        }).collect(Collectors.toList()));
        res.setVariants(product.getVariants().stream().map(this::mapVariant).collect(Collectors.toList()));
        return res;
    }

    private VariantResponse mapVariant(ProductVariant variant) {
        VariantResponse res = new VariantResponse();
        res.setId(variant.getId());
        res.setSku(variant.getSku());
        res.setAttributes(variant.getAttributes());
        res.setPrice(variant.getPrice());
        res.setStock(variant.getStock());
        return res;
    }

    private void applyFlashSale(Product product, Boolean enabled, BigDecimal price, LocalDateTime startAt, LocalDateTime endAt) {
        boolean flashSaleEnabled = Boolean.TRUE.equals(enabled);
        if (!flashSaleEnabled) {
            product.setFlashSaleEnabled(false);
            product.setFlashSalePrice(null);
            product.setFlashSaleStartAt(null);
            product.setFlashSaleEndAt(null);
            return;
        }
        if (price == null) {
            throw new BusinessException("Flash sale can gia khuyen mai");
        }
        if (startAt == null || endAt == null) {
            throw new BusinessException("Flash sale can thoi gian bat dau va ket thuc");
        }
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException("Thoi gian ket thuc flash sale phai sau thoi gian bat dau");
        }
        if (price.compareTo(product.getBasePrice()) >= 0) {
            throw new BusinessException("Gia flash sale phai nho hon gia goc");
        }
        product.setFlashSaleEnabled(true);
        product.setFlashSalePrice(price);
        product.setFlashSaleStartAt(startAt);
        product.setFlashSaleEndAt(endAt);
    }

    private String normalizeStatus(String status, String fallback) {
        String normalized = status == null || status.isBlank() ? fallback : status.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ACTIVE") && !normalized.equals("INACTIVE") && !normalized.equals("DELETED")) {
            throw new BusinessException("Status san pham khong hop le");
        }
        return normalized;
    }

    private boolean isFlashSaleActive(Product product) {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(product.getFlashSaleEnabled())
                && product.getFlashSalePrice() != null
                && product.getFlashSaleStartAt() != null
                && product.getFlashSaleEndAt() != null
                && !now.isBefore(product.getFlashSaleStartAt())
                && now.isBefore(product.getFlashSaleEndAt());
    }

    private BigDecimal resolveEffectivePrice(Product product) {
        return isFlashSaleActive(product) ? product.getFlashSalePrice() : product.getBasePrice();
    }

    private String resolvePrimaryImage(Product product) {
        return product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());
    }

    private String generateSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("").toLowerCase();
        return slug.replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-");
    }
}
