package com.ecommerce.service;

import com.ecommerce.dto.request.order.CreateOrderRequest;
import com.ecommerce.dto.response.order.OrderDetailResponse;
import com.ecommerce.dto.response.order.OrderListResponse;
import com.ecommerce.dto.response.order.OrderStatusHistoryResponse;
import com.ecommerce.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    // Tạo đơn hàng mới từ giỏ hàng (customer)
    OrderDetailResponse createOrder(CreateOrderRequest request, Long customerId);

    // Lấy chi tiết đơn hàng (customer)
    OrderDetailResponse getOrderDetail(Long orderId, Long customerId);

    // Danh sách đơn hàng của khách hàng (phân trang)
    Page<OrderListResponse> getCustomerOrders(Long customerId, Pageable pageable);

    // Lấy chi tiết đơn hàng cho seller
    OrderDetailResponse getShopOrderDetail(Long orderId, Long shopId);

    // Danh sách đơn hàng của shop (phân trang)
    Page<OrderListResponse> getShopOrders(Long shopId, Pageable pageable);

    // Cập nhật trạng thái đơn hàng (seller)
    OrderDetailResponse updateOrderStatus(Long orderId, OrderStatus newStatus, Long sellerId);

    // Hủy đơn hàng - seller hủy đơn của shop
    OrderDetailResponse cancelShopOrder(Long orderId, Long shopId);

    // Hủy đơn hàng - customer hủy đơn của mình
    OrderDetailResponse cancelOrder(Long orderId, Long customerId);

    // Lấy lịch sử trạng thái của đơn hàng
    List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId);
}

