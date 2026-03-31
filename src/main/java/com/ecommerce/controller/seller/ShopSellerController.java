package com.ecommerce.controller.seller;

import com.ecommerce.dto.request.shop.ShopRegistrationRequest;
import com.ecommerce.dto.request.shop.ShopUpdateRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.shop.ShopResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/shops")
@RequiredArgsConstructor
public class ShopSellerController {

    private final ShopService shopService;

    // Đăng ký tạo shop mới
    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()") // Hoặc hasAnyRole('CUSTOMER', 'SELLER')
    public ApiResponse<ShopResponse> registerShop(
            @AuthenticationPrincipal User user, // Giả sử UserDetails implementation là User entity
            @Valid @RequestBody ShopRegistrationRequest request) {
        
        // sellerId lấy trực tiếp từ token để đảm bảo tính xác thực
        return ApiResponse.created(shopService.createShop(user.getId(), request));
    }

    // Cập nhật thông tin shop (Dành cho chủ shop)
    @PutMapping("/update")
    @PreAuthorize("hasRole('SELLER')") // Chỉ người có quyền SELLER (hoặc đang duyệt) mới được sửa, tùy logic policy, ta dùng isAuthenticated cho đơn giản hoặc check Role.
    public ApiResponse<ShopResponse> updateShop(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ShopUpdateRequest request) {
        
        return ApiResponse.success(shopService.updateShopInfo(user.getId(), request));
    }

    // Xem thông tin shop của chính mình
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ShopResponse> getMyShop(@AuthenticationPrincipal User user) {
        return ApiResponse.success(shopService.getShopBySellerId(user.getId()));
    }
}
