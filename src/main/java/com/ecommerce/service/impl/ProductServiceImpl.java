package com.ecommerce.service.impl;

import com.ecommerce.dto.request.product.CreateProductRequest;
import com.ecommerce.dto.request.product.CreateVariantRequest;
import com.ecommerce.dto.request.product.UpdateProductRequest;
import com.ecommerce.dto.response.product.ProductDetailResponse;
import com.ecommerce.dto.response.product.ProductImageResponse;
import com.ecommerce.dto.response.product.ProductResponse;
import com.ecommerce.dto.response.product.VariantResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

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
        
        // Auto-gen temporary slug
        product.setSlug(generateSlug(req.getName()) + "-" + System.currentTimeMillis());

        // Save to get ID
        product = productRepository.save(product);
        
        // Update correct slug with ID
        product.setSlug(generateSlug(req.getName()) + "-" + product.getId());

        int totalStock = 0;

        // Process images
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setImageUrl(req.getImageUrls().get(i));
                image.setIsPrimary(i == 0);
                product.getImages().add(image);
            }
        }

        // Process variants
        if (req.getVariants() != null && !req.getVariants().isEmpty()) {
            for (CreateVariantRequest vReq : req.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setAttributes(vReq.getAttributes());
                variant.setPrice(vReq.getPrice());
                variant.setStock(vReq.getStock() != null ? vReq.getStock() : 0);
                variant.setSku(vReq.getSku());
                product.getVariants().add(variant);
                totalStock += variant.getStock();
            }
        }

        product.setStockQuantity(totalStock);
        product = productRepository.save(product);

        return mapToDetailResponse(product);
    }

    @Override
    public Page<ProductResponse> getShopProducts(Long shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Lấy danh sách sản phẩm của shop, không bao gồm DELETED
        Page<Product> products = productRepository.findByShopIdAndStatusNot(shopId, "DELETED", pageable);
        return products.map(this::mapToResponse);
    }

    @Override
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
            throw new BusinessException("Sản phẩm đã bị xóa");
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

        // Xử lý images (Xóa cũ, thay mới)
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            product.getImages().clear();
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setImageUrl(req.getImageUrls().get(i));
                image.setIsPrimary(i == 0);
                product.getImages().add(image);
            }
        }

        // Xử lý variants (Xóa cũ, thay mới)
        if (req.getVariants() != null) {
            product.getVariants().clear();
            int totalStock = 0;
            for (CreateVariantRequest vReq : req.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setAttributes(vReq.getAttributes());
                variant.setPrice(vReq.getPrice());
                variant.setStock(vReq.getStock() != null ? vReq.getStock() : 0);
                variant.setSku(vReq.getSku());
                product.getVariants().add(variant);
                totalStock += variant.getStock();
            }
            product.setStockQuantity(totalStock);
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

    // Helper methods for mapping
    private ProductResponse mapToResponse(Product product) {
        ProductResponse res = new ProductResponse();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setBasePrice(product.getBasePrice());
        
        // Find primary image
        String primaryImg = product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());
                
        res.setPrimaryImageUrl(primaryImg);
        res.setStockQuantity(product.getStockQuantity());
        res.setStatus(product.getStatus());
        res.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        res.setRating(product.getRating());
        res.setSoldCount(product.getSoldCount());
        res.setCreatedAt(product.getCreatedAt());
        return res;
    }

    private ProductDetailResponse mapToDetailResponse(Product product) {
        ProductDetailResponse res = new ProductDetailResponse();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setBasePrice(product.getBasePrice());
        
        String primaryImg = product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());
                
        res.setPrimaryImageUrl(primaryImg);
        res.setStockQuantity(product.getStockQuantity());
        res.setStatus(product.getStatus());
        res.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        res.setRating(product.getRating());
        res.setSoldCount(product.getSoldCount());
        res.setCreatedAt(product.getCreatedAt());
        
        res.setDescription(product.getDescription());
        res.setSlug(product.getSlug());
        res.setShopId(product.getShop().getId());
        res.setShopName(product.getShop().getName());
        
        List<ProductImageResponse> imgRes = product.getImages().stream().map(img -> {
            ProductImageResponse pir = new ProductImageResponse();
            pir.setId(img.getId());
            pir.setImageUrl(img.getImageUrl());
            pir.setIsPrimary(img.getIsPrimary());
            return pir;
        }).collect(Collectors.toList());
        res.setImages(imgRes);
        
        List<VariantResponse> varRes = product.getVariants().stream().map(v -> {
            VariantResponse vr = new VariantResponse();
            vr.setId(v.getId());
            vr.setSku(v.getSku());
            vr.setAttributes(v.getAttributes());
            vr.setPrice(v.getPrice());
            vr.setStock(v.getStock());
            return vr;
        }).collect(Collectors.toList());
        res.setVariants(varRes);
        
        return res;
    }

    private String generateSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("").toLowerCase();
        slug = slug.replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-");
        return slug;
    }
}
