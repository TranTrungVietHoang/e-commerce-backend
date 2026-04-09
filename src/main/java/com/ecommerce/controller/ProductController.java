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
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ecommerce.service.UserService;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.exception.BusinessException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Slf4j
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final com.ecommerce.service.FileService fileService;
    private final UserService userService;
    private final ShopRepository shopRepository;

    private Long getShopIdFromUser(UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return shopRepository.findFirstBySellerId(userId)
                .orElseThrow(() -> new BusinessException("Bạn chưa mở shop hoặc shop không tồn tại"))
                .getId();
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> uploadImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String url = fileService.uploadFile(file);
            return ResponseEntity.ok(ApiResponse.success(url));
        } catch (java.io.IOException e) {
            return new ResponseEntity<>(ApiResponse.error(500, "Lỗi upload ảnh: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateProductRequest request) {
        Long shopId = getShopIdFromUser(userDetails);
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
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProductRequest request) {
        Long shopId = getShopIdFromUser(userDetails);
        ProductDetailResponse response = productService.updateProduct(id, request, shopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/seller/{id}/status")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateProductStatusForSeller(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String status) {
        Long shopId = getShopIdFromUser(userDetails);
        productService.updateProductStatusBySeller(id, shopId, status);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long shopId = getShopIdFromUser(userDetails);
        productService.softDeleteProduct(id, shopId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Inventory APIs
    @PatchMapping("/variants/{variantId}/stock")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VariantResponse>> updateVariantStock(
            @PathVariable Long variantId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateVariantStockRequest request) {
        Long shopId = getShopIdFromUser(userDetails);
        VariantResponse response = productService.updateVariantStock(variantId, request.getStock(), shopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<LowStockVariantResponse>>> getLowStockVariants(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long shopId = getShopIdFromUser(userDetails);
        List<LowStockVariantResponse> response = productService.getLowStockVariants(shopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
