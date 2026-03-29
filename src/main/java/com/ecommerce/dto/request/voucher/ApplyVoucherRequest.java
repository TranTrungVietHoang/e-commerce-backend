package com.ecommerce.dto.request.voucher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplyVoucherRequest {
    @NotBlank(message = "Ma voucher khong duoc trong")
    private String code;

    private Long shopId;

    @NotNull(message = "Gia tri don hang khong duoc trong")
    private BigDecimal orderValue;
}
