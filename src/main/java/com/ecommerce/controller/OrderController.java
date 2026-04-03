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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateOrderRequest request) {
        try {
            Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
            log.info("Tạo đơn hàng: customerId={}", customerId);
            OrderDetailResponse response = orderService.createOrder(request, customerId);
            return new ResponseEntity<>(ApiResponse.success(response, "Tạo đơn hàng thành công"), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Lỗi tạo đơn hàng: {}", e.getMessage(), e);
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

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
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<OrderListResponse> orderPage = orderService.getCustomerOrders(customerId, pageable);
            return new ResponseEntity<>(ApiResponse.success(orderPage), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách đơn: {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/shop/{shopId}/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getShopOrderDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long shopId,
            @PathVariable Long orderId) {
        try {
            log.info("Lấy chi tiết đơn của shop: shopId={}, orderId={}", shopId, orderId);
            OrderDetailResponse response = orderService.getShopOrderDetail(orderId, shopId);
            return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy chi tiết đơn shop: {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> getShopOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<OrderListResponse> orderPage = orderService.getShopOrders(shopId, pageable);
            return new ResponseEntity<>(ApiResponse.success(orderPage), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách đơn shop: {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateOrderStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        try {
            Long sellerId = userService.getUserIdByUsername(userDetails.getUsername());
            log.info("Cập nhật trạng thái đơn: orderId={}, status={}", orderId, request.getStatus());
            OrderDetailResponse response = orderService.updateOrderStatus(orderId, request.getStatus(), sellerId);
            return new ResponseEntity<>(ApiResponse.success(response, "Cập nhật trạng thái thành công"), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi cập nhật trạng thái: {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/shop/{shopId}/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelShopOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long shopId,
            @PathVariable Long orderId) {
        try {
            OrderDetailResponse response = orderService.cancelShopOrder(orderId, shopId);
            return new ResponseEntity<>(ApiResponse.success(response, "Hủy đơn hàng thành công"), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi hủy đơn hàng shop: {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        try {
            Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
            OrderDetailResponse response = orderService.cancelOrder(orderId, customerId);
            return new ResponseEntity<>(ApiResponse.success(response, "Hủy đơn hàng thành công"), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi hủy đơn hàng: {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{orderId}/status-history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderStatusHistory(
            @PathVariable Long orderId) {
        try {
            List<OrderStatusHistoryResponse> history = orderService.getOrderStatusHistory(orderId);
            return new ResponseEntity<>(ApiResponse.success(history), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy lịch sử trạng thái: {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/verify-purchase/{productId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPurchase(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        try {
            Long userId = userService.getUserIdByUsername(userDetails.getUsername());
            Long orderItemId = orderService.checkPurchase(userId, productId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("canReview", orderItemId != null);
            result.put("orderItemId", orderItemId);
            
            return new ResponseEntity<>(ApiResponse.success(result), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi xác thực mua hàng: {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.error(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}
