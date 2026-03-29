package com.ecommerce.dto.request.voucher;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVoucherRequest {
    @NotBlank(message = "Ma voucher khong duoc trong")
    private String code;

    @NotBlank(message = "Ten voucher khong duoc trong")
    private String name;

    private String description;

    @NotBlank(message = "Loai giam gia khong duoc trong")
    private String discountType;

    @NotNull(message = "Gia tri giam khong duoc trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gia tri giam phai lon hon 0")
    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscountValue;

    private Integer usageLimit;

    @NotNull(message = "Thieu thoi gian bat dau")
    private LocalDateTime startAt;

    @NotNull(message = "Thieu thoi gian ket thuc")
    private LocalDateTime endAt;

    private Boolean active;
}
