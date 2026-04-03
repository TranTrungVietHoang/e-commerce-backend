package com.ecommerce.dto.response.product;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal basePrice;
    private String primaryImageUrl;
    private Integer stockQuantity;
    private String status;
    private String categoryName;
    private BigDecimal rating;
    private Long soldCount;
    private LocalDateTime createdAt;
    private BigDecimal effectivePrice;
    private Boolean flashSaleActive;
    private BigDecimal flashSalePrice;
    private LocalDateTime flashSaleStartAt;
    private LocalDateTime flashSaleEndAt;
    private String statusReason;
}
