package com.ecommerce.service;

import com.ecommerce.dto.request.payment.CreatePaymentQrRequest;
import com.ecommerce.dto.response.payment.PaymentQrResponse;

public interface PaymentService {
    PaymentQrResponse createPaymentQr(CreatePaymentQrRequest request);
    void confirmPaymentWebhook(String transactionId, String signature);
    PaymentQrResponse getPaymentStatus(Long orderId);
}
