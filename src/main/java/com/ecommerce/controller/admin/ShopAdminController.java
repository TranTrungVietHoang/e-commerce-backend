package com.ecommerce.controller.admin;

import com.ecommerce.dto.request.shop.ShopApprovalRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.shop.ShopResponse;
import com.ecommerce.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
public class ShopAdminController {

    private final ShopService shopService;

    // Lấy danh sách tất cả các Shop đang chờ duyệt (PENDING)
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ShopResponse>> getPendingShops() {
        return ApiResponse.success(shopService.getPendingShops());
    }

    // Phê duyệt hoặc Từ chối Shop (Approve/Reject)
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> approveShop(@PathVariable Long id, @Valid @RequestBody ShopApprovalRequest request) {
        shopService.approveShop(id, request.getStatus());
        return ApiResponse.success(null, "Cập nhật trạng thái duyệt cửa hàng thành công");
    }
    
    // Lấy danh sách tất cả Shop (Để quản lý chung)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ShopResponse>> getAllShops() {
        return ApiResponse.success(shopService.getAllShops());
    }
}
