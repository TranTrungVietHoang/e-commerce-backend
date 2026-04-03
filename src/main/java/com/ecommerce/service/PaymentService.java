package com.ecommerce.service;

import com.ecommerce.dto.request.payment.CreatePaymentQrRequest;
import com.ecommerce.dto.response.payment.PaymentQrResponse;

/**
 * Interface cho Payment/Thanh toán - hỗ trợ nhiều payment gateway
 */
public interface PaymentService {
    /**
     * Tạo QR code thanh toán via Sepay
     */
    PaymentQrResponse createPaymentQr(CreatePaymentQrRequest request);

    /**
     * Xác nhận thanh toán webhook từ Sepay
     */
    void confirmPaymentWebhook(String transactionId, String signature);

    /**
     * Kiểm tra status thanh toán
     */
    PaymentQrResponse getPaymentStatus(Long orderId);
}
