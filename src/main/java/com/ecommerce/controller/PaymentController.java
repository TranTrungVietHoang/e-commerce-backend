package com.ecommerce.controller;

import com.ecommerce.dto.request.payment.CreatePaymentQrRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.payment.PaymentQrResponse;
import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Payment API - Xử lý thanh toán (Sepay, COD, ...)
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @org.springframework.beans.factory.annotation.Value("${sepay.webhook-secret:}")
    private String webhookSecret;

    /**
     * Tạo QR code thanh toán Sepay
     * POST /api/v1/payments/sepay/create-qr
     */
    @PostMapping("/sepay/create-qr")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentQrResponse>> createSepayQr(
            @RequestBody CreatePaymentQrRequest request) {
        PaymentQrResponse response = paymentService.createPaymentQr(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Webhook từ Sepay khi user thanh toán
     * POST /api/v1/payments/sepay/webhook
     */
    @PostMapping("/sepay/webhook")
    public ResponseEntity<ApiResponse<Void>> sepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody java.util.Map<String, Object> payload) {
        
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            if (authorization == null || !authorization.contains(webhookSecret)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }

        // SePay gửi webhook dạng JSON body
        String content = payload.containsKey("content") ? payload.get("content").toString().toUpperCase() : "";
        String transactionId = payload.containsKey("id") ? payload.get("id").toString() : "";
        
        Long orderId = null;
        try {
            // Extract mã đơn hàng (ví dụ: THANH-TOAN-10048 hoặc THANHTOAN10048)
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("THANH-?TOAN-?\\s*(\\d+)").matcher(content);
            if (m.find()) {
                orderId = Long.parseLong(m.group(1));
            }
        } catch (Exception e) {
            // ignore
        }

        // Gọi service xử lý (truyền orderId vào dạng string handle tạm)
        if (orderId != null) {
            paymentService.confirmPaymentWebhook(orderId.toString(), transactionId);
        } else {
            System.out.println("Không tìm thấy mã đơn hàng trong nội dung CK: " + content);
        }
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Kiểm tra status thanh toán
     * GET /api/v1/payments/status/{orderId}
     */
    @GetMapping("/status/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentQrResponse>> getPaymentStatus(
            @PathVariable Long orderId) {
        PaymentQrResponse response = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
