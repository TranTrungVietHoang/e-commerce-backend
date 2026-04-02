package com.ecommerce.dto.response.voucher;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherApplyResponse {
    private String code;
    private String discountType; // PERCENT hoặc FIXED_AMOUNT
    private BigDecimal discountValue; // giá trị % hoặc tiền để hiển thị
    private BigDecimal orderValue;
    private BigDecimal discountAmount; // số tiền được giảm thực tế
    private BigDecimal finalAmount;
}
