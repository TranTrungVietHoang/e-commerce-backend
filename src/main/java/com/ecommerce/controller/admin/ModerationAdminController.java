package com.ecommerce.controller.admin;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.product.ProductResponse;
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ModerationAdminController {

    private final ProductService productService;

    @GetMapping("/products")
    public ApiResponse<Page<ProductResponse>> getPendingProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(productService.getPendingProducts(page, size));
    }

    @PatchMapping("/products/{id}")
    public ApiResponse<String> moderateProduct(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status"); // APPROVED hoac REJECTED
        productService.moderateProduct(id, status);
        return ApiResponse.success("Cập nhật trạng thái phê duyệt thành công");
    }
}
