package com.ecommerce.service;

import com.ecommerce.dto.request.order.CreateOrderRequest;
import com.ecommerce.dto.response.order.OrderDetailResponse;
import com.ecommerce.dto.response.order.OrderListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    /**
     * Tạo đơn hàng mới từ giỏ hàng
     * - Kiểm tra stock variant
     * - Tính toán giá (apply voucher nếu có)
     * - Trừ kho
     * - Lưu order + order_items
     */
    OrderDetailResponse createOrder(CreateOrderRequest request, Long customerId);

    /**
     * Lấy chi tiết đơn hàng
     */
    OrderDetailResponse getOrderDetail(Long orderId, Long customerId);

    /**
     * Danh sách đơn hàng của khách hàng
     */
    Page<OrderListResponse> getCustomerOrders(Long customerId, Pageable pageable);

    /**
     * Danh sách đơn hàng của shop (cho seller)
     */
    Page<OrderListResponse> getShopOrders(Long shopId, Pageable pageable);

    /**
     * Chi tiết đơn hàng của shop (cho seller)
     */
    OrderDetailResponse getShopOrderDetail(Long orderId, Long shopId);

    /**
     * Cập nhật trạng thái đơn (Seller only)
     * PENDING -> CONFIRMED -> SHIPPING -> DELIVERED
     */
    OrderDetailResponse updateOrderStatus(Long orderId, String newStatus, Long sellerId);

    /**
     * Hủy đơn hàng (khách hàng hủy đơn chưa confirm)
     */
    OrderDetailResponse cancelOrder(Long orderId, Long customerId);

    /**
     * Hủy đơn hàng (seller hủy đơn ở trạng thái PENDING hoặc CONFIRMED)
     */
    OrderDetailResponse cancelShopOrder(Long orderId, Long shopId);

    /**
     * Xem lịch sử trạng thái của đơn
     */
    List<com.ecommerce.dto.response.order.OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId);

    /**
     * @return orderItemId nếu hợp lệ, null nếu chưa mua/chưa giao
     */
    Long checkPurchase(Long userId, Long productId);

    /**
     * Lấy tất cả đơn hàng hệ thống (cho Admin)
     */
    Page<OrderListResponse> getAllOrders(Pageable pageable);
}
