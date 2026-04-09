package com.ecommerce.service.impl;

import com.ecommerce.config.SepayConfig;
import com.ecommerce.dto.request.payment.CreatePaymentQrRequest;
import com.ecommerce.dto.response.payment.PaymentQrResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Sepay Payment Gateway Integration
 * API Docs: https://my.sepay.vn/api/v2/doc
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SepayPaymentServiceImpl implements PaymentService {

    private final SepayConfig sepayConfig;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    /**
     * Tạo QR code thanh toán Sepay
     * - Gọi Sepay API để sinh QR
     * - Lưu transaction ID vào database
     * - Trả về QR response cho frontend
     */
    @Override
    @Transactional
    public PaymentQrResponse createPaymentQr(CreatePaymentQrRequest request) {
        // Validation
        if (request.getOrderId() == null || request.getAmount() == null) {
            throw new IllegalArgumentException("Order ID và Amount không được bỏ trống");
        }

        // Lấy order từ DB
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId()));

        // Format nội dung chuyển khoản
        String transferContent = String.format("THANH-TOAN-%d", request.getOrderId());

        // Call Sepay API (mock cho đơn giản)
        PaymentQrResponse response = callSepayAPI(request, transferContent);

        log.info("Tạo QR thanh toán Sepay cho Order: {}, Amount: {}", 
                 request.getOrderId(), request.getAmount());

        return response;
    }

    /**
     * Xác nhận webhook từ Sepay khi user thanh toán thành công
     */
    @Override
    @Transactional
    public void confirmPaymentWebhook(String transactionId, String signature) {
        log.info("Nhận webhook Sepay với Order ID: {}, Sepay ID: {}", transactionId, signature);
        
        try {
            Long orderId = Long.parseLong(transactionId);
            Order order = orderRepository.findById(orderId).orElse(null);
            
            if (order != null && "PENDING".equals(order.getStatus())) {
                order.setStatus("PAID"); // HOẶC COMPLETED Tùy hệ thống
                orderRepository.save(order);
                log.info("Thành công: Đã cập nhật trạng thái Order {} thành PAID", orderId);
            } else {
                log.warn("Bỏ qua Webhook: Order {} không tồn tại hoặc đã được xử lý", orderId);
            }
        } catch (NumberFormatException e) {
            log.error("Lỗi parse Order ID từ TransactionID webhook", e);
        }
    }

    /**
     * Kiểm tra status thanh toán
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentQrResponse getPaymentStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        
        return PaymentQrResponse.builder()
                .orderId(orderId)
                .status(order.getStatus())
                .build();
    }

    /**
     * Call Sepay API (Mock implementation)
     * Trong production, bạn cần:
     * 1. Call https://my.sepay.vn/api/v2/generateqr
     * 2. Verify API response signature
     * 3. Lưu transaction reference vào DB
     */
    private PaymentQrResponse callSepayAPI(CreatePaymentQrRequest request, String transferContent) {
        Map<String, Object> sepayRequest = new HashMap<>();
        sepayRequest.put("bank_account_number", request.getAccountNo() != null ? request.getAccountNo() : "0334088130");
        sepayRequest.put("bank_account_name", request.getAccountName() != null ? request.getAccountName() : "VO MINH TAM");
        sepayRequest.put("amount", request.getAmount().intValue());
        sepayRequest.put("description", transferContent);
        sepayRequest.put("reference_code", "ORDER-" + request.getOrderId());
        
        log.debug("Sepay API Request: {}", sepayRequest);
        
        try {
            // Gọi Sepay API thực tế
            Map response = restTemplate.postForObject(
                sepayConfig.getApiUrl() + "/generateqr",
                sepayRequest,
                Map.class
            );
            
            log.info("Sepay API Response: {}", response);
            
            if (response != null && response.containsKey("data")) {
                Map qrData = (Map) response.get("data");
                return PaymentQrResponse.builder()
                        .orderId(request.getOrderId())
                        .amount(request.getAmount())
                        .transferContent(transferContent)
                        .qrCodeBase64((String) qrData.get("qr_code"))
                        .expiresAt(Instant.now().plusSeconds(900).toEpochMilli()) // 15 phút
                        .status("PENDING")
                        .build();
            }
        } catch (Exception e) {
            log.error("Lỗi gọi Sepay API: {}", e.getMessage(), e);
        }
        
        // Fallback: Tạo URL VietQR ảnh thông qua Sepay (Free tier standard)
        String template = "https://qr.sepay.vn/img?bank=%s&acc=%s&amount=%d&des=%s";
        String qrUrl = String.format(template, 
                request.getBankCode() != null ? request.getBankCode() : "MB",
                request.getAccountNo() != null ? request.getAccountNo() : "0334088130",
                request.getAmount().intValue(),
                transferContent);
        
        return PaymentQrResponse.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .transferContent(transferContent)
                .qrCode(qrUrl)
                .expiresAt(Instant.now().plusSeconds(900).toEpochMilli()) // 15 phút
                .status("PENDING")
                .build();
    }
}
