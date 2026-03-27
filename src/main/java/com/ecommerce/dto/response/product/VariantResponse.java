package com.ecommerce.dto.response.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantResponse {
    private Long id;
    private String sku;
    private String attributes;
    private BigDecimal price;
    private Integer stock;
}
