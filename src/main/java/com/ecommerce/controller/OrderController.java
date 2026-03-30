package com.ecommerce.controller;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.request.order.CreateOrderRequest;
import com.ecommerce.dto.request.order.UpdateOrderStatusRequest;
import com.ecommerce.dto.response.order.OrderDetailResponse;
import com.ecommerce.dto.response.order.OrderListResponse;
import com.ecommerce.dto.response.order.OrderStatusHistoryResponse;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * POST /api/v1/orders
     * Tạo đơn hàng mới từ giỏ hàng
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateOrderRequest request) {
        try {
            Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
            log.info("Tạo đơn hàng: customerId={}, shopId={}", customerId, request.getShopId());
            
            OrderDetailResponse response = orderService.createOrder(request, customerId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Tạo đơn hàng thành công"));
        } catch (Exception e) {
            log.error("Lỗi tạo đơn hàng: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * GET /api/v1/orders/{orderId}
     * Lấy chi tiết đơn hàng
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        try {
            Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
            OrderDetailResponse response = orderService.getOrderDetail(orderId, customerId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Lỗi lấy chi tiết đơn: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * GET /api/v1/orders
     * Danh sách đơn hàng của khách hàng
     * Query params: page, size (default: page=0, size=10)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderListResponse> orderPage = orderService.getCustomerOrders(customerId, pageable);
            return ResponseEntity.ok(ApiResponse.success(orderPage));
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách đơn: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * GET /api/v1/orders/shop/{shopId}
     * Danh sách đơn hàng của shop (cho seller)
     */
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> getShopOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = userService.getUserIdByUsername(userDetails.getUsername());
            log.info("Lấy danh sách đơn của shop: shopId={}, userId={}", shopId, userId);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderListResponse> orderPage = orderService.getShopOrders(shopId, pageable);
            return ResponseEntity.ok(ApiResponse.success(orderPage));
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách đơn shop: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * PUT /api/v1/orders/{orderId}/status
     * Cập nhật trạng thái đơn hàng (seller only)
     * Body: { "status": "CONFIRMED" | "SHIPPING" | "DELIVERED" }
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateOrderStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        try {
            Long sellerId = userService.getUserIdByUsername(userDetails.getUsername());
            log.info("Cập nhật trạng thái đơn: orderId={}, sellerId={}, status={}", orderId, sellerId, request.getStatus());
            
            OrderDetailResponse response = orderService.updateOrderStatus(orderId, request.getStatus(), sellerId);
            return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật trạng thái thành công"));
        } catch (Exception e) {
            log.error("Lỗi cập nhật trạng thái: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * DELETE /api/v1/orders/{orderId}
     * Hủy đơn hàng (khách hàng only, chỉ hủy được đơn ở trạng thái PENDING)
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        try {
            Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
            log.info("Hủy đơn hàng: orderId={}, customerId={}", orderId, customerId);
            
            OrderDetailResponse response = orderService.cancelOrder(orderId, customerId);
            return ResponseEntity.ok(ApiResponse.success(response, "Hủy đơn hàng thành công"));
        } catch (Exception e) {
            log.error("Lỗi hủy đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * GET /api/v1/orders/{orderId}/status-history
     * Xem lịch sử thay đổi trạng thái của đơn
     */
    @GetMapping("/{orderId}/status-history")
    public ResponseEntity<ApiResponse<java.util.List<OrderStatusHistoryResponse>>> getOrderStatusHistory(
            @PathVariable Long orderId) {
        try {
            java.util.List<OrderStatusHistoryResponse> history = orderService.getOrderStatusHistory(orderId);
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            log.error("Lỗi lấy lịch sử trạng thái: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    // ==================== PRIVATE METHODS ====================

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                    (org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal();
            // Assume username is email or ID
            String username = userDetails.getUsername();
            // TODO: Implement proper user ID retrieval from JWT token
            return Long.parseLong(username);
        }
        throw new RuntimeException("Không thể xác định người dùng");
    }

    // ==================== DTO CLASSES ====================

    @lombok.Data
    public static class UpdateOrderStatusRequest {
        private String status; // CONFIRMED, SHIPPING, DELIVERED, CANCELLED
    }
}
