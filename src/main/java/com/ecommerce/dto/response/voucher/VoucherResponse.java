package com.ecommerce.dto.response.voucher;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountValue;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean active;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long shopId;
}
