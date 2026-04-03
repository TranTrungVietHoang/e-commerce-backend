package com.ecommerce.dto.response.order;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderListResponse {

    private Long id;

    // Thông tin shop
    private Long shopId;
    private String shopName;

    // Trạng thái
    private OrderStatus status;

    // Thông tin giao hàng
    private String recipientName;

    // Tài chính
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;

    // Số lượng sản phẩm
    private Integer totalItems;

    // Thời gian
    private LocalDateTime createdAt;
}

