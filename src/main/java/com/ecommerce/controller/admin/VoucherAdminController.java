package com.ecommerce.controller.admin;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.voucher.VoucherResponse;
import com.ecommerce.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class VoucherAdminController {

    private final VoucherService voucherService;

    @GetMapping
    public ApiResponse<List<VoucherResponse>> getAllVouchers() {
        log.info("Admin request: Lấy tất cả voucher hệ thống");
        return ApiResponse.success(voucherService.getAllVouchers());
    }
}
