package com.ecommerce.dto.response.revenue;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class TopProductResponse implements Serializable {
    private Long productId;
    private String productName;
    private Long soldCount;
    private BigDecimal totalRevenue;
}
