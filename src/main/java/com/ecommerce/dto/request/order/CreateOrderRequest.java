package com.ecommerce.dto.request.order;

import com.ecommerce.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    // Danh sách ID các CartItem muốn đặt hàng (nếu đặt từ giỏ hàng)
    private List<Long> cartItemIds;

    // ID của shop (nếu đặt trực tiếp)
    private Long shopId;

    // Thông tin giao hàng
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;

    // Phương thức thanh toán
    private PaymentMethod paymentMethod;

    // Mã voucher hoặc ID voucher
    private Long voucherId;
    private String voucherCode;

    // Điểm thưởng và ghi chú
    private Integer pointsUsed;
    private String note;
}
