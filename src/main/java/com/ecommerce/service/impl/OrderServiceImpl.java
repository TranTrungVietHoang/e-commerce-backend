package com.ecommerce.service.impl;

import com.ecommerce.dto.request.order.CreateOrderRequest;
import com.ecommerce.dto.response.order.OrderDetailResponse;
import com.ecommerce.dto.response.order.OrderItemResponse;
import com.ecommerce.dto.response.order.OrderListResponse;
import com.ecommerce.dto.response.order.OrderStatusHistoryResponse;
import com.ecommerce.entity.*;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.*;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    // =========================================================
    // Tạo đơn hàng
    // =========================================================

    @Override
    @Transactional
    public OrderDetailResponse createOrder(CreateOrderRequest request, Long customerId) {
        // Validate đầu vào
        if (request.getCartItemIds() == null || request.getCartItemIds().isEmpty()) {
            throw new BusinessException("Danh sách sản phẩm không được trống");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw new BusinessException("Địa chỉ giao hàng không được trống");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Lấy các cart items
        List<CartItem> cartItems = cartItemRepository.findAllById(request.getCartItemIds());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Không tìm thấy sản phẩm trong giỏ hàng");
        }

        // Xác định shop từ sản phẩm đầu tiên (trong một đơn hàng chỉ có 1 shop)
        Shop shop = cartItems.get(0).getProduct().getShop();

        // Tính tổng tiền
        BigDecimal subtotal = cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = BigDecimal.valueOf(30000); // Phí ship cố định 30.000đ
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount);

        // Tạo Order
        Order order = Order.builder()
                .customer(customer)
                .shop(shop)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .recipientName(request.getRecipientName() != null ? request.getRecipientName() : customer.getFullName())
                .recipientPhone(request.getRecipientPhone() != null ? request.getRecipientPhone() : customer.getPhone())
                .paymentMethod(request.getPaymentMethod())
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .voucherCode(request.getVoucherCode())
                .note(request.getNote())
                .build();

        order = orderRepository.save(order);

        // Tạo OrderItems từ CartItems và trừ kho
        final Order savedOrder = order;
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            // Trừ kho
            if (variant != null) {
                if (variant.getStock() < cartItem.getQuantity()) {
                    throw new BusinessException("Sản phẩm '" + product.getName() + "' không đủ số lượng trong kho");
                }
                variant.setStock(variant.getStock() - cartItem.getQuantity());
            } else {
                if (product.getStockQuantity() < cartItem.getQuantity()) {
                    throw new BusinessException("Sản phẩm '" + product.getName() + "' không đủ số lượng trong kho");
                }
                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            }

            // Lấy ảnh đại diện
            String imageUrl = product.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(product.getImages().stream()
                            .findFirst()
                            .map(ProductImage::getImageUrl)
                            .orElse(null));

            BigDecimal lineTotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            return OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .variant(variant)
                    .productName(product.getName())
                    .variantName(variant != null ? variant.getAttributes() : null)
                    .imageUrl(imageUrl)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build();
        }).collect(Collectors.toList());

        order.setItems(orderItems);
        order = orderRepository.save(order);

        // Ghi lịch sử trạng thái
        saveStatusHistory(order, null, OrderStatus.PENDING, customerId, "Đơn hàng được tạo");

        // Xóa các CartItem đã đặt hàng khỏi giỏ
        cartItemRepository.deleteAll(cartItems);

        return mapToDetailResponse(order);
    }

    // =========================================================
    // Customer: xem đơn
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đơn hàng #" + orderId));
        return mapToDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListResponse> getCustomerOrders(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToListResponse);
    }

    // =========================================================
    // Seller: xem đơn
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getShopOrderDetail(Long orderId, Long shopId) {
        Order order = orderRepository.findByIdAndShopId(orderId, shopId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đơn hàng #" + orderId + " trong shop"));
        return mapToDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListResponse> getShopOrders(Long shopId, Pageable pageable) {
        return orderRepository.findByShopId(shopId, pageable)
                .map(this::mapToListResponse);
    }

    // =========================================================
    // Seller: cập nhật trạng thái đơn
    // =========================================================

    @Override
    @Transactional
    public OrderDetailResponse updateOrderStatus(Long orderId, OrderStatus newStatus, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đơn hàng #" + orderId));

        // Validate chuyển trạng thái hợp lệ
        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order = orderRepository.save(order);

        saveStatusHistory(order, oldStatus, newStatus, sellerId, "Seller cập nhật trạng thái");

        return mapToDetailResponse(order);
    }

    // =========================================================
    // Seller: hủy đơn
    // =========================================================

    @Override
    @Transactional
    public OrderDetailResponse cancelShopOrder(Long orderId, Long shopId) {
        Order order = orderRepository.findByIdAndShopId(orderId, shopId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đơn hàng #" + orderId + " trong shop"));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("Chỉ có thể hủy đơn ở trạng thái PENDING hoặc CONFIRMED");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        restoreStock(order);
        order = orderRepository.save(order);

        saveStatusHistory(order, oldStatus, OrderStatus.CANCELLED, shopId, "Seller hủy đơn");

        return mapToDetailResponse(order);
    }

    // =========================================================
    // Customer: hủy đơn
    // =========================================================

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đơn hàng #" + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Chỉ có thể hủy đơn ở trạng thái PENDING");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        restoreStock(order);
        order = orderRepository.save(order);

        saveStatusHistory(order, oldStatus, OrderStatus.CANCELLED, customerId, "Khách hàng hủy đơn");

        return mapToDetailResponse(order);
    }

    // =========================================================
    // Lịch sử trạng thái
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng #" + orderId);
        }
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    // =========================================================
    // Helper methods
    // =========================================================

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED  -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPED;
            case SHIPPED    -> next == OrderStatus.DELIVERED;
            default         -> false;
        };
        if (!valid) {
            throw new BusinessException(
                    "Không thể chuyển trạng thái từ " + current + " sang " + next);
        }
    }

    private void restoreStock(Order order) {
        order.getItems().forEach(item -> {
            if (item.getVariant() != null) {
                item.getVariant().setStock(item.getVariant().getStock() + item.getQuantity());
            } else {
                item.getProduct().setStockQuantity(
                        item.getProduct().getStockQuantity() + item.getQuantity());
            }
        });
    }

    private void saveStatusHistory(Order order,
                                   OrderStatus oldStatus,
                                   OrderStatus newStatus,
                                   Long changedBy,
                                   String note) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .note(note)
                .build();
        orderStatusHistoryRepository.save(history);
    }

    // =========================================================
    // Mapper methods
    // =========================================================

    private OrderDetailResponse mapToDetailResponse(Order order) {
        OrderDetailResponse resp = new OrderDetailResponse();
        resp.setId(order.getId());
        resp.setCustomerId(order.getCustomer().getId());
        resp.setCustomerName(order.getCustomer().getFullName());
        resp.setShopId(order.getShop().getId());
        resp.setShopName(order.getShop().getName());
        resp.setStatus(order.getStatus());
        resp.setShippingAddress(order.getShippingAddress());
        resp.setRecipientName(order.getRecipientName());
        resp.setRecipientPhone(order.getRecipientPhone());
        resp.setPaymentMethod(order.getPaymentMethod());
        resp.setSubtotal(order.getSubtotal());
        resp.setDiscountAmount(order.getDiscountAmount());
        resp.setShippingFee(order.getShippingFee());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setVoucherCode(order.getVoucherCode());
        resp.setNote(order.getNote());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    OrderItemResponse itemResp = new OrderItemResponse();
                    itemResp.setId(item.getId());
                    itemResp.setProductId(item.getProduct().getId());
                    itemResp.setVariantId(item.getVariant() != null ? item.getVariant().getId() : null);
                    itemResp.setProductName(item.getProductName());
                    itemResp.setVariantName(item.getVariantName());
                    itemResp.setImageUrl(item.getImageUrl());
                    itemResp.setQuantity(item.getQuantity());
                    itemResp.setUnitPrice(item.getUnitPrice());
                    itemResp.setLineTotal(item.getLineTotal());
                    return itemResp;
                })
                .collect(Collectors.toList());
        resp.setItems(itemResponses);

        return resp;
    }

    private OrderListResponse mapToListResponse(Order order) {
        OrderListResponse resp = new OrderListResponse();
        resp.setId(order.getId());
        resp.setShopId(order.getShop().getId());
        resp.setShopName(order.getShop().getName());
        resp.setStatus(order.getStatus());
        resp.setRecipientName(order.getRecipientName());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setPaymentMethod(order.getPaymentMethod());
        resp.setTotalItems(order.getItems().stream()
                .mapToInt(OrderItem::getQuantity).sum());
        resp.setCreatedAt(order.getCreatedAt());
        return resp;
    }

    private OrderStatusHistoryResponse mapToHistoryResponse(OrderStatusHistory h) {
        OrderStatusHistoryResponse resp = new OrderStatusHistoryResponse();
        resp.setId(h.getId());
        resp.setOrderId(h.getOrder().getId());
        resp.setOldStatus(h.getOldStatus());
        resp.setNewStatus(h.getNewStatus());
        resp.setChangedBy(h.getChangedBy());
        resp.setNote(h.getNote());
        resp.setChangedAt(h.getChangedAt());
        return resp;
    }
}
