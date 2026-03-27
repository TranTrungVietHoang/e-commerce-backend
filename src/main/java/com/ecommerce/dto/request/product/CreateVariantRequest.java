package com.ecommerce.dto.request.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateVariantRequest {
    @NotBlank(message = "Thuộc tính biến thể không được để trống")
    private String attributes; // JSON: {"Màu":"Đỏ","Size":"L"}

    @NotNull(message = "Giá biến thể không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá biến thể phải lớn hơn 0")
    private BigDecimal price;

    @Min(value = 0, message = "Kho không được âm")
    private Integer stock;

    private String sku;
}
