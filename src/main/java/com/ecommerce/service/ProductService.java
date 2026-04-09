package com.ecommerce.service;

import com.ecommerce.dto.request.product.CreateProductRequest;
import com.ecommerce.dto.request.product.UpdateProductRequest;
import com.ecommerce.dto.response.product.LowStockVariantResponse;
import com.ecommerce.dto.response.product.ProductDetailResponse;
import com.ecommerce.dto.response.product.ProductResponse;
import com.ecommerce.dto.response.product.VariantResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    ProductDetailResponse createProduct(CreateProductRequest req, Long shopId);
    Page<ProductResponse> getShopProducts(Long shopId, int page, int size);
    List<ProductResponse> getPublicProducts();
    ProductDetailResponse getProductById(Long id);
    ProductDetailResponse updateProduct(Long id, UpdateProductRequest req, Long shopId);
    void softDeleteProduct(Long id, Long shopId);
    VariantResponse updateVariantStock(Long variantId, int newStock, Long shopId);
    List<LowStockVariantResponse> getLowStockVariants(Long shopId);

    // Moderation
    Page<ProductResponse> getPendingProducts(int page, int size);
    void moderateProduct(Long id, String status);
    void updateProductStatusBySeller(Long id, Long shopId, String status);
}
