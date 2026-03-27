package com.ecommerce.dto.response.voucher;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherApplyResponse {
    private String code;
    private BigDecimal orderValue;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}
