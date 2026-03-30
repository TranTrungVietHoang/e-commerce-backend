package com.ecommerce.dto.request.order;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long shopId;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String paymentMethod; // COD, SEPAY_TRANSFER
    private Long voucherId;
    private Integer pointsUsed;
}
