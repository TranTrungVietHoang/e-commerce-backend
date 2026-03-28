package com.ecommerce.dto.response.cart;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponse {
    private Long cartId;
    private Long userId;
    private Integer totalItems;
    private BigDecimal subtotal;
    private List<CartItemResponse> items;
}
