package com.ecommerce.dto.response.product;

import lombok.Data;

@Data
public class LowStockVariantResponse {
    private Long variantId;
    private String sku;
    private String attributes;
    private int stock;
    private Long productId;
    private String productName;
}
