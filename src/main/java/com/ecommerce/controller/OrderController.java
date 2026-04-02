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
            return new ResponseEntity<>(ApiResponse.success(response, "Tạo đơn hàng thành công"), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Lỗi tạo đơn hàng: {}", e.getMessage(), e);
            @SuppressWarnings("unchecked")
            ApiResponse<OrderDetailResponse> errorResponse = (ApiResponse<OrderDetailResponse>) (Object) ApiResponse.error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
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
            return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy chi tiết đơn: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<OrderDetailResponse> errorResponse = (ApiResponse<OrderDetailResponse>) (Object) ApiResponse.error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
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
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
            Page<OrderListResponse> orderPage = orderService.getCustomerOrders(customerId, pageable);
            return new ResponseEntity<>(ApiResponse.success(orderPage), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách đơn: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<Page<OrderListResponse>> errorResponse = (ApiResponse<Page<OrderListResponse>>) (Object) ApiResponse.error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/v1/orders/shop/{shopId}/{orderId}
     * Chi tiết đơn hàng của shop (cho seller) - MUST come before getShopOrders
     */
    @GetMapping("/shop/{shopId}/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getShopOrderDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long shopId,
            @PathVariable Long orderId) {
        try {
            Long userId = userService.getUserIdByUsername(userDetails.getUsername());
            log.info("Lấy chi tiết đơn của shop: shopId={}, orderId={}, userId={}", shopId, orderId, userId);
            
            // Lấy chi tiết đơn hàng cho seller
            OrderDetailResponse response = orderService.getShopOrderDetail(orderId, shopId);
            return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy chi tiết đơn shop: orderId={}, shopId={}, error: {}", orderId, shopId, e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<OrderDetailResponse> errorResponse = (ApiResponse<OrderDetailResponse>) (Object) ApiResponse.error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
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
            
            Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
            Page<OrderListResponse> orderPage = orderService.getShopOrders(shopId, pageable);
            return new ResponseEntity<>(ApiResponse.success(orderPage), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách đơn shop: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<Page<OrderListResponse>> errorResponse = (ApiResponse<Page<OrderListResponse>>) (Object) ApiResponse.error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
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
            return new ResponseEntity<>(ApiResponse.success(response, "Cập nhật trạng thái thành công"), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi cập nhật trạng thái: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<OrderDetailResponse> errorResponse = (ApiResponse<OrderDetailResponse>) (Object) ApiResponse.error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * PUT /api/v1/orders/shop/{shopId}/{orderId}/cancel
     * Hủy đơn hàng (seller only, hủy được đơn ở trạng thái PENDING hoặc CONFIRMED)
     */
    @PutMapping("/shop/{shopId}/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelShopOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long shopId,
            @PathVariable Long orderId) {
        try {
            Long userId = userService.getUserIdByUsername(userDetails.getUsername());
            log.info("Hủy đơn hàng shop: orderId={}, shopId={}, userId={}", orderId, shopId, userId);
            
            OrderDetailResponse response = orderService.cancelShopOrder(orderId, shopId);
            return new ResponseEntity<>(ApiResponse.success(response, "Hủy đơn hàng thành công"), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi hủy đơn hàng shop: orderId={}, shopId={}, error: {}", orderId, shopId, e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<OrderDetailResponse> errorResponse = (ApiResponse<OrderDetailResponse>) (Object) ApiResponse.error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
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
            return new ResponseEntity<>(ApiResponse.success(response, "Hủy đơn hàng thành công"), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi hủy đơn hàng: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<OrderDetailResponse> errorResponse = (ApiResponse<OrderDetailResponse>) (Object) ApiResponse.error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/v1/orders/{orderId}/status-history
     * Xem lịch sử thay đổi trạng thái của đơn
     */
    @GetMapping("/{orderId}/status-history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderStatusHistory(
            @PathVariable Long orderId) {
        try {
            List<OrderStatusHistoryResponse> history = orderService.getOrderStatusHistory(orderId);
            return new ResponseEntity<>(ApiResponse.success(history), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy lịch sử trạng thái: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<List<OrderStatusHistoryResponse>> errorResponse = (ApiResponse<List<OrderStatusHistoryResponse>>) (Object) ApiResponse.error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
