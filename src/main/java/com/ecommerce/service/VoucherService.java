package com.ecommerce.service;

import com.ecommerce.dto.request.voucher.ApplyVoucherRequest;
import com.ecommerce.dto.request.voucher.CreateVoucherRequest;
import com.ecommerce.dto.response.voucher.VoucherApplyResponse;
import com.ecommerce.dto.response.voucher.VoucherResponse;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    List<VoucherResponse> getAvailableVouchers(Long shopId, BigDecimal orderValue);
    VoucherApplyResponse applyVoucher(Long userId, ApplyVoucherRequest request);
    VoucherApplyResponse consumeVoucher(Long userId, ApplyVoucherRequest request);
    List<VoucherResponse> getSellerVouchers(Long shopId);
    VoucherResponse createVoucher(Long shopId, CreateVoucherRequest request);
    VoucherResponse updateVoucher(Long voucherId, Long shopId, CreateVoucherRequest request);
    void deleteVoucher(Long voucherId, Long shopId);

    /**
     * Lấy tất cả voucher hệ thống (cho Admin)
     */
    List<VoucherResponse> getAllVouchers();
}
