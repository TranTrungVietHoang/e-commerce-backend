package com.ecommerce.controller;

import com.ecommerce.dto.request.voucher.ApplyVoucherRequest;
import com.ecommerce.dto.request.voucher.CreateVoucherRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.voucher.VoucherApplyResponse;
import com.ecommerce.dto.response.voucher.VoucherResponse;
import com.ecommerce.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ecommerce.service.UserService;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.exception.BusinessException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final UserService userService;
    private final ShopRepository shopRepository;

    private Long getShopIdFromUser(UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return shopRepository.findBySellerId(userId)
                .orElseThrow(() -> new BusinessException("Bạn chưa mở shop hoặc shop không tồn tại"))
                .getId();
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getAvailableVouchers(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "0") BigDecimal orderValue) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getAvailableVouchers(shopId, orderValue)));
    }

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VoucherApplyResponse>> applyVoucher(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplyVoucherRequest request) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(voucherService.applyVoucher(userId, request)));
    }

    @PostMapping("/consume")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VoucherApplyResponse>> consumeVoucher(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplyVoucherRequest request) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(voucherService.consumeVoucher(userId, request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getSellerVouchers(@AuthenticationPrincipal UserDetails userDetails) {
        Long shopId = getShopIdFromUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(voucherService.getSellerVouchers(shopId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateVoucherRequest request) {
        Long shopId = getShopIdFromUser(userDetails);
        return new ResponseEntity<>(ApiResponse.created(voucherService.createVoucher(shopId, request)), HttpStatus.CREATED);
    }

    @PutMapping("/{voucherId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable Long voucherId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateVoucherRequest request) {
        Long shopId = getShopIdFromUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(voucherService.updateVoucher(voucherId, shopId, request)));
    }

    @DeleteMapping("/{voucherId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Long voucherId, @AuthenticationPrincipal UserDetails userDetails) {
        Long shopId = getShopIdFromUser(userDetails);
        voucherService.deleteVoucher(voucherId, shopId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
