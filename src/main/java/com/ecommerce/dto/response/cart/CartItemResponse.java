package com.ecommerce.dto.response.cart;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemResponse {
    private Long id;
    private Long shopId;
    private String shopName;
    private Long productId;
    private Long variantId;
    private String productName;
    private String variantName;
    private String imageUrl;
    private Integer quantity;
    private Integer availableStock;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private BigDecimal basePrice;
    private BigDecimal flashSalePrice;
    private Boolean flashSaleActive;
    private LocalDateTime flashSaleEndAt;
}
