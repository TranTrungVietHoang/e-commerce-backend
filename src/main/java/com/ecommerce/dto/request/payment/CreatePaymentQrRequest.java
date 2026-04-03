package com.ecommerce.dto.request.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request để tạo QR code thanh toán Sepay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentQrRequest {
    private Long orderId;
    private BigDecimal amount;
    private String description;
    private String accountNo;      // Số tài khoản nhận
    private String accountName;    // Tên chủ tài khoản
    private String bankCode;       // Mã ngân hàng (VCB, TCB, etc)
}
