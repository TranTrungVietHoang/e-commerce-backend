package com.ecommerce.dto.response.product;

import lombok.Data;

@Data
public class ProductImageResponse {
    private Long id;
    private String imageUrl;
    private Boolean isPrimary;
}
