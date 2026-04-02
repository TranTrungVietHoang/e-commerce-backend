package com.ecommerce.dto.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response chứa QR code thanh toán từ Sepay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentQrResponse {
    private Long orderId;
    private String qrCode;           // QR code URL hoặc base64
    private String qrCodeBase64;     // Base64 encoded QR
    private String transferContent;  // Nội dung chuyển khoản
    private Long expiresAt;          // Hết hạn lúc (timestamp)
    private String status;           // PENDING, PAID, EXPIRED, CANCELLED
    private BigDecimal amount;       // Số tiền thanh toán
}
