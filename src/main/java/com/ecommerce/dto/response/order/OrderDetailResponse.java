package com.ecommerce.dto.response.order;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private Long id;

    // Thông tin khách hàng
    private Long customerId;
    private String customerName;

    // Thông tin shop
    private Long shopId;
    private String shopName;

    // Trạng thái đơn hàng
    private OrderStatus status;

    // Thông tin giao hàng
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;

    // Thanh toán
    private PaymentMethod paymentMethod;

    // Tiền
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private Integer pointsUsed;

    private String voucherCode;
    private String note;

    // Danh sách sản phẩm
    private List<OrderItemResponse> items;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
