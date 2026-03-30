package com.ecommerce.controller;

import com.ecommerce.entity.Shop;
import com.ecommerce.enums.ShopStatus;
import com.ecommerce.service.ShopService;
import com.ecommerce.service.impl.ShopServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Tag(name = "Shop Management", description = "APIs đăng ký và quản lý gian hàng")
public class ShopController {

    private final ShopService shopService;

    @PostMapping("/register/{sellerId}")
    @Operation(summary = "Khách hàng đăng ký mở Shop")
    public ResponseEntity<Object> register(@PathVariable Long sellerId, @RequestBody Shop request) {
        return ResponseEntity.ok(shopService.createShop(sellerId, request));
    }

    @GetMapping("/{shopId}")
    @Operation(summary = "Xem thông tin chi tiết một Shop")
    public ResponseEntity<Object> getInfo(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.getShopInfo(shopId));
    }

    @PutMapping("/{shopId}")
    @Operation(summary = "Cập nhật thông tin Shop (Seller)")
    public ResponseEntity<Object> update(@PathVariable Long shopId, @RequestBody Shop request) {
        return ResponseEntity.ok(shopService.updateShopInfo(shopId, request));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách các Shop (Phân trang - Admin)")
    public ResponseEntity<Object> getAll(Pageable pageable) {
        return ResponseEntity.ok(shopService.getShops(pageable));
    }

    @PatchMapping("/{shopId}/status")
    @Operation(summary = "Phê duyệt hoặc từ chối Shop (Admin)")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long shopId, 
            @RequestParam ShopStatus status) {
        // Ép kiểu để gọi hàm duyệt shop trong Impl
        ((ShopServiceImpl) shopService).approveShop(shopId, status);
        return ResponseEntity.ok("Cập nhật trạng thái Shop thành công!");
    }
}