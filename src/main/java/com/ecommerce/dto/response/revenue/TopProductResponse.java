package com.ecommerce.dto.response.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse implements Serializable {
    private Long productId;
    private String productName;
    private Long soldCount;
    private BigDecimal totalRevenue;
}
