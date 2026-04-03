package com.ecommerce.controller;

import com.ecommerce.dto.request.product.CreateProductRequest;
import com.ecommerce.dto.request.product.UpdateProductRequest;
import com.ecommerce.dto.request.product.UpdateVariantStockRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.product.LowStockVariantResponse;
import com.ecommerce.dto.response.product.ProductDetailResponse;
import com.ecommerce.dto.response.product.ProductResponse;
import com.ecommerce.dto.response.product.VariantResponse;
import com.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Slf4j
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final com.ecommerce.service.FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<?>> uploadImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String url = fileService.uploadFile(file);
            return ResponseEntity.ok(ApiResponse.success(url));
        } catch (java.io.IOException e) {
            return new ResponseEntity<>(ApiResponse.error(500, "Lỗi upload ảnh: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @RequestParam Long shopId,
            @Valid @RequestBody CreateProductRequest request) {
        ProductDetailResponse response = productService.createProduct(request, shopId);
        return new ResponseEntity<>(ApiResponse.created(response), HttpStatus.CREATED);
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getShopProducts(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProductResponse> response = productService.getShopProducts(shopId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPublicProducts() {
        log.info("🔍 GET /api/v1/products/public - Fetching public products");
        List<ProductResponse> products = productService.getPublicProducts();
        log.info("✅ Found {} public products", products.size());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable Long id) {
        ProductDetailResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long id,
            @RequestParam Long shopId,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductDetailResponse response = productService.updateProduct(id, request, shopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @RequestParam Long shopId) {
        productService.softDeleteProduct(id, shopId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Inventory APIs
    @PatchMapping("/variants/{variantId}/stock")
    public ResponseEntity<ApiResponse<VariantResponse>> updateVariantStock(
            @PathVariable Long variantId,
            @RequestParam Long shopId,
            @Valid @RequestBody UpdateVariantStockRequest request) {
        VariantResponse response = productService.updateVariantStock(variantId, request.getStock(), shopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<LowStockVariantResponse>>> getLowStockVariants(
            @RequestParam Long shopId) {
        List<LowStockVariantResponse> response = productService.getLowStockVariants(shopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Admin APIs
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts(page, size)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProductStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProductStatus(id, status, reason)));
    }

    @PutMapping("/seller/{id}/status")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProductStatusForSeller(
            @PathVariable Long id,
            @RequestParam Long shopId,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProductStatusForSeller(id, status, shopId)));
    }
}
