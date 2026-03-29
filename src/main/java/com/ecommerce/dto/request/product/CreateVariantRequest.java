package com.ecommerce.dto.request.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateVariantRequest {
    private Long id;

    @NotBlank(message = "Thuoc tinh bien the khong duoc de trong")
    private String attributes;

    @NotNull(message = "Gia bien the khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gia bien the phai lon hon 0")
    private BigDecimal price;

    @Min(value = 0, message = "Kho khong duoc am")
    private Integer stock;

    private String sku;
}
