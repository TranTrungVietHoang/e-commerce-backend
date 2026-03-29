package com.ecommerce.dto.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {
    @NotNull(message = "Thieu productId")
    private Long productId;

    private Long variantId;

    @NotNull(message = "Thieu so luong")
    @Min(value = 1, message = "So luong phai lon hon 0")
    private Integer quantity;
}
