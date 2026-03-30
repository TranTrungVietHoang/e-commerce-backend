package com.ecommerce.dto.response.order;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailResponse implements Serializable {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long shopId;
    private String shopName;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Integer pointsUsed;
    private String status; // PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;
    private String paymentMethod; // COD, SEPAY_TRANSFER
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDTO> items;

    @Data
    @Builder
    public static class OrderItemDTO implements Serializable {
        private Long id;
        private Long productId;
        private String productName;
        private Long variantId;
        private String variantAttributes;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
