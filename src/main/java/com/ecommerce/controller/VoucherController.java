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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getAvailableVouchers(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "0") BigDecimal orderValue) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getAvailableVouchers(shopId, orderValue)));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<VoucherApplyResponse>> applyVoucher(
            @RequestParam Long userId,
            @Valid @RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.applyVoucher(userId, request)));
    }

    @PostMapping("/consume")
    public ResponseEntity<ApiResponse<VoucherApplyResponse>> consumeVoucher(
            @RequestParam Long userId,
            @Valid @RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.consumeVoucher(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getSellerVouchers(@RequestParam Long shopId) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getSellerVouchers(shopId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @RequestParam Long shopId,
            @Valid @RequestBody CreateVoucherRequest request) {
        return new ResponseEntity<>(ApiResponse.created(voucherService.createVoucher(shopId, request)), HttpStatus.CREATED);
    }

    @PutMapping("/{voucherId}")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable Long voucherId,
            @RequestParam Long shopId,
            @Valid @RequestBody CreateVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.updateVoucher(voucherId, shopId, request)));
    }

    @DeleteMapping("/{voucherId}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Long voucherId, @RequestParam Long shopId) {
        voucherService.deleteVoucher(voucherId, shopId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
