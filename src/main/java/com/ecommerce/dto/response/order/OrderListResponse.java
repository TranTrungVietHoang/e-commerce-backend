package com.ecommerce.dto.response.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderListResponse implements Serializable {
    private Long id;
    private String shopName;
    private Integer itemCount;
    private BigDecimal totalAmount;
    private String status; // PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED
    private LocalDateTime createdAt;
}
