package com.ecommerce.dto.request.product;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateVariantStockRequest {
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private int stock;
}
