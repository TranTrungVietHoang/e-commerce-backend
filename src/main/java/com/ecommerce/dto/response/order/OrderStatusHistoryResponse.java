package com.ecommerce.dto.response.order;

import com.ecommerce.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderStatusHistoryResponse {

    private Long id;
    private Long orderId;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private Long changedBy;
    private String note;
    private LocalDateTime changedAt;
}

