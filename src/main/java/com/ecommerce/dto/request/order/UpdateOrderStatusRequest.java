package com.ecommerce.dto.request.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    private String status; // PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED
}
