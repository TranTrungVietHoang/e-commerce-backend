package com.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PointHistoryResponse {
    private Long id;
    private Long userId;
    private Long orderId;
    private Integer points;
    private String type; // EARN, REDEEM
    private String description;
    private LocalDateTime createdAt;
}
