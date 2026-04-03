package com.ecommerce.dto.request.order;

import com.ecommerce.enums.OrderStatus;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    private OrderStatus status;
    private String note;
}

