package com.ecommerce.controller;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.request.order.CreateOrderRequest;
import com.ecommerce.dto.request.order.UpdateOrderStatusRequest;
import com.ecommerce.dto.response.order.OrderDetailResponse;
import com.ecommerce.dto.response.order.OrderListResponse;
import com.ecommerce.dto.response.order.OrderStatusHistoryResponse;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.UserService;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final ShopRepository shopRepository;

    private void verifyShopOwnership(Long requestedShopId, UserDetails userDetails) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        Long sellerShopId = shopRepository.findFirstBySellerId(userId)
                .orElseThrow(() -> new BusinessException("B?n chua m? shop"))
                .getId();
        if (!sellerShopId.equals(requestedShopId)) {
            throw new BusinessException("Không có quy?n thao tác trên don hàng shop này");
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateOrderRequest request) {
        Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
        log.info("T?o don hàng: customerId={}, shopId={}", customerId, request.getShopId());
        
        OrderDetailResponse response = orderService.createOrder(request, customerId);
        return new ResponseEntity<>(ApiResponse.success(response, "T?o don hàng thành công"), HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
        OrderDetailResponse response = orderService.getOrderDetail(orderId, customerId);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<OrderListResponse> orderPage = orderService.getCustomerOrders(customerId, pageable);
        return new ResponseEntity<>(ApiResponse.success(orderPage), HttpStatus.OK);
    }

    @GetMapping("/shop/{shopId}/{orderId}")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getShopOrderDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long shopId,
            @PathVariable Long orderId) {
        verifyShopOwnership(shopId, userDetails);
        log.info("L?y chi ti?t don c?a shop: shopId={}, orderId={}", shopId, orderId);
        
        OrderDetailResponse response = orderService.getShopOrderDetail(orderId, shopId);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    @GetMapping("/shop/{shopId}")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> getShopOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        verifyShopOwnership(shopId, userDetails);
        log.info("L?y danh sách don c?a shop: shopId={}", shopId);
        
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<OrderListResponse> orderPage = orderService.getShopOrders(shopId, pageable);
        return new ResponseEntity<>(ApiResponse.success(orderPage), HttpStatus.OK);
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateOrderStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        Long sellerId = userService.getUserIdByUsername(userDetails.getUsername());
        log.info("C?p nh?t tr?ng thái don: orderId={}, sellerId={}, status={}", orderId, sellerId, request.getStatus());
        
        OrderDetailResponse response = orderService.updateOrderStatus(orderId, request.getStatus(), sellerId);
        return new ResponseEntity<>(ApiResponse.success(response, "C?p nh?t tr?ng thái thành công"), HttpStatus.OK);
    }

    @PutMapping("/shop/{shopId}/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelShopOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long shopId,
            @PathVariable Long orderId) {
        verifyShopOwnership(shopId, userDetails);
        log.info("H?y don hàng shop: orderId={}, shopId={}", orderId, shopId);
        
        OrderDetailResponse response = orderService.cancelShopOrder(orderId, shopId);
        return new ResponseEntity<>(ApiResponse.success(response, "H?y don hàng thành công"), HttpStatus.OK);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        Long customerId = userService.getUserIdByUsername(userDetails.getUsername());
        log.info("H?y don hàng: orderId={}, customerId={}", orderId, customerId);
        
        OrderDetailResponse response = orderService.cancelOrder(orderId, customerId);
        return new ResponseEntity<>(ApiResponse.success(response, "H?y don hàng thành công"), HttpStatus.OK);
    }

    @GetMapping("/{orderId}/status-history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderStatusHistory(
            @PathVariable Long orderId) {
        List<OrderStatusHistoryResponse> history = orderService.getOrderStatusHistory(orderId);
        return new ResponseEntity<>(ApiResponse.success(history), HttpStatus.OK);
    }

    @GetMapping("/verify-purchase/{productId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPurchase(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        Long orderItemId = orderService.checkPurchase(userId, productId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("canReview", orderItemId != null);
        result.put("orderItemId", orderItemId);
        
        return new ResponseEntity<>(ApiResponse.success(result), HttpStatus.OK);
    }
}

