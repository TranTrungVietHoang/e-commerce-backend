package com.ecommerce.service;

import com.ecommerce.dto.request.product.CreateProductRequest;
import com.ecommerce.dto.request.product.UpdateProductRequest;
import com.ecommerce.dto.response.product.ProductDetailResponse;
import com.ecommerce.dto.response.product.ProductResponse;
import org.springframework.data.domain.Page;

public interface ProductService {
    ProductDetailResponse createProduct(CreateProductRequest req, Long shopId);
    Page<ProductResponse> getShopProducts(Long shopId, int page, int size);
    ProductDetailResponse getProductById(Long id);
    ProductDetailResponse updateProduct(Long id, UpdateProductRequest req, Long shopId);
    void softDeleteProduct(Long id, Long shopId);
    
    // Inventory methods
    com.ecommerce.dto.response.product.VariantResponse updateVariantStock(Long variantId, int newStock, Long shopId);
    java.util.List<com.ecommerce.dto.response.product.LowStockVariantResponse> getLowStockVariants(Long shopId);
}
