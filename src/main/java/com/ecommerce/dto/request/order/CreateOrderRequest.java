package com.ecommerce.dto.request.order;

import com.ecommerce.enums.PaymentMethod;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    // Danh sách ID các CartItem muốn đặt hàng
    private List<Long> cartItemIds;

    // Thông tin giao hàng
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;

    // Phương thức thanh toán
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    // Mã voucher (tuỳ chọn)
    private String voucherCode;

    // Ghi chú đơn hàng (tuỳ chọn)
    private String note;
}

