package com.ecommerce.service.impl;

import com.ecommerce.dto.request.order.CreateOrderRequest;
import com.ecommerce.dto.response.order.OrderDetailResponse;
import com.ecommerce.dto.response.order.OrderItemResponse;
import com.ecommerce.dto.response.order.OrderListResponse;
import com.ecommerce.dto.response.order.OrderStatusHistoryResponse;
import com.ecommerce.entity.*;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentMethod;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    public OrderDetailResponse createOrder(CreateOrderRequest request, Long customerId) {
        log.info("Tạo đơn hàng mới: customerId={}", customerId);

        // 1. Validate đầu vào & Lấy thông tin khách hàng
        if (request.getCartItemIds() == null || request.getCartItemIds().isEmpty()) {
            throw new BusinessException("Danh sách sản phẩm đặt hàng không được trống");
        }
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", customerId));

        // 2. Lấy danh sách CartItems
        List<CartItem> cartItems = cartItemRepository.findAllById(request.getCartItemIds());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Không tìm thấy các sản phẩm yêu cầu trong giỏ hàng");
        }

        // 3. Xác định Shop (giả định 1 order cho 1 shop)
        Shop shop = cartItems.get(0).getProduct().getShop();

        // 4. Kiểm tra kho & Tính toán subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            if (variant != null) {
                if (variant.getStock() < cartItem.getQuantity()) {
                    throw new BusinessException("Sản phẩm " + product.getName() + " (mẫu mã) không đủ hàng");
                }
            } else {
                if (product.getStockQuantity() < cartItem.getQuantity()) {
                    throw new BusinessException("Sản phẩm " + product.getName() + " không đủ hàng");
                }
            }

            BigDecimal itemTotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        // 5. Tính toán giảm giá (Voucher)
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher voucher = null;
        if (request.getVoucherId() != null || request.getVoucherCode() != null) {
            if (request.getVoucherId() != null) {
                voucher = voucherRepository.findById(request.getVoucherId()).orElse(null);
            } else {
                voucher = voucherRepository.findByCodeIgnoreCase(request.getVoucherCode()).orElse(null);
            }

            if (voucher != null) {
                if (!isVoucherValid(voucher, subtotal)) {
                    throw new BusinessException("Voucher không hợp lệ hoặc không đáp ứng điều kiện");
                }
                
                if ("PERCENT".equals(voucher.getDiscountType())) {
                    discountAmount = subtotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
                } else {
                    discountAmount = voucher.getDiscountValue();
                }

                if (voucher.getMaxDiscountValue() != null) {
                    discountAmount = discountAmount.min(voucher.getMaxDiscountValue());
                }
                
                // Cập nhật số lần sử dụng voucher
                voucher.setUsedCount(voucher.getUsedCount() + 1);
                voucherRepository.save(voucher);
            }
        }

        // 6. Tính toán phí vận chuyển (Tạm thời cố định 30k)
        BigDecimal shippingFee = BigDecimal.valueOf(30000);

        // 7. Tính tổng tiền cuối cùng
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // 8. Tạo đơn hàng (Order)
        Order order = Order.builder()
                .customer(customer)
                .shop(shop)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .recipientName(request.getRecipientName() != null ? request.getRecipientName() : customer.getFullName())
                .recipientPhone(request.getRecipientPhone() != null ? request.getRecipientPhone() : customer.getPhone())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.COD)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .pointsUsed(request.getPointsUsed() != null ? request.getPointsUsed() : 0)
                .voucherCode(voucher != null ? voucher.getCode() : request.getVoucherCode())
                .note(request.getNote())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 9. Tạo chi tiết đơn hàng (OrderItems) & Trừ kho
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            // Trừ kho
            if (variant != null) {
                variant.setStock(variant.getStock() - cartItem.getQuantity());
                productVariantRepository.save(variant);
            } else {
                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(product);
            }

            // Mapper ảnh và thông tin snapshot
            String imageUrl = getProductPrimaryImage(product);

            return OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .variant(variant)
                    .productName(product.getName())
                    .variantName(variant != null ? variant.getAttributes() : null)
                    .imageUrl(imageUrl)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .lineTotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();
        }).collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        // 10. Lưu lịch sử trạng thái ban đầu
        saveStatusHistory(savedOrder, null, OrderStatus.PENDING, customerId, "Đơn hàng được tạo");

        // 11. Dọn dẹp giỏ hàng
        cartItemRepository.deleteAll(cartItems);

        log.info("Tạo đơn hàng thành công: orderId={}", savedOrder.getId());
        return mapToDetailResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng #" + orderId));
        return mapToDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListResponse> getCustomerOrders(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListResponse> getShopOrders(Long shopId, Pageable pageable) {
        return orderRepository.findByShopId(shopId, pageable)
                .map(this::mapToListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getShopOrderDetail(Long orderId, Long shopId) {
        Order order = orderRepository.findByIdAndShopId(orderId, shopId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng #" + orderId + " trong shop"));
        return mapToDetailResponse(order);
    }

    @Override
    public OrderDetailResponse updateOrderStatus(Long orderId, OrderStatus newStatus, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Quyền Seller
        if (!order.getShop().getSeller().getId().equals(sellerId)) {
            throw new BusinessException("Bạn không có quyền cập nhật đơn hàng này");
        }

        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        saveStatusHistory(updatedOrder, oldStatus, newStatus, sellerId, "Seller cập nhật trạng thái");
        log.info("Cập nhật trạng thái đơn: orderId={}, {} -> {}", orderId, oldStatus, newStatus);

        return mapToDetailResponse(updatedOrder);
    }

    @Override
    public OrderDetailResponse cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Chỉ có thể hủy đơn ở trạng thái CHỜ XÁC NHẬN");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        restoreStock(order);
        Order updatedOrder = orderRepository.save(order);

        saveStatusHistory(updatedOrder, oldStatus, OrderStatus.CANCELLED, customerId, "Khách hàng hủy đơn");
        return mapToDetailResponse(updatedOrder);
    }

    @Override
    public OrderDetailResponse cancelShopOrder(Long orderId, Long shopId) {
        Order order = orderRepository.findByIdAndShopId(orderId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("Chỉ có thể hủy đơn khi chưa giao vận");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        restoreStock(order);
        Order updatedOrder = orderRepository.save(order);

        saveStatusHistory(updatedOrder, oldStatus, OrderStatus.CANCELLED, shopId, "Shop hủy đơn");
        return mapToDetailResponse(updatedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId)
                .stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long checkPurchase(Long userId, Long productId) {
        List<OrderItem> items = orderItemRepository.findDeliveredItemByUserAndProduct(userId, productId);
        if (items != null && !items.isEmpty()) {
            return items.get(0).getId();
        }
        return null;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED  -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPED;
            case SHIPPED    -> next == OrderStatus.DELIVERED;
            default         -> false;
        };
        if (!valid) {
            throw new BusinessException("Không thể chuyển trạng thái từ " + current + " sang " + next);
        }
    }

    private void restoreStock(Order order) {
        order.getItems().forEach(item -> {
            if (item.getVariant() != null) {
                item.getVariant().setStock(item.getVariant().getStock() + item.getQuantity());
                productVariantRepository.save(item.getVariant());
            } else {
                item.getProduct().setStockQuantity(item.getProduct().getStockQuantity() + item.getQuantity());
                productRepository.save(item.getProduct());
            }
        });
    }

    private void saveStatusHistory(Order order, OrderStatus oldStatus, OrderStatus newStatus, Long changedBy, String note) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .note(note)
                .build();
        orderStatusHistoryRepository.save(history);
    }

    private boolean isVoucherValid(Voucher voucher, BigDecimal subtotal) {
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getExpiresAt() != null && now.isAfter(voucher.getExpiresAt())) return false;
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) return false;
        if (voucher.getMinOrderValue() != null && subtotal.compareTo(voucher.getMinOrderValue()) < 0) return false;
        return true;
    }

    private String getProductPrimaryImage(Product product) {
        return product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().stream().findFirst().map(ProductImage::getImageUrl).orElse(null));
    }

    // ==================== RESPONSE MAPPERS ====================

    private OrderDetailResponse mapToDetailResponse(Order order) {
        return OrderDetailResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFullName())
                .shopId(order.getShop().getId())
                .shopName(order.getShop().getName())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .paymentMethod(order.getPaymentMethod())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .pointsUsed(order.getPointsUsed())
                .voucherCode(order.getVoucherCode())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream().map(this::mapToItemResponse).collect(Collectors.toList()))
                .build();
    }

    private OrderItemResponse mapToItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .imageUrl(item.getImageUrl())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private OrderListResponse mapToListResponse(Order order) {
        return OrderListResponse.builder()
                .id(order.getId())
                .shopId(order.getShop().getId())
                .shopName(order.getShop().getName())
                .status(order.getStatus())
                .recipientName(order.getRecipientName())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .itemCount(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderStatusHistoryResponse mapToHistoryResponse(OrderStatusHistory h) {
        return OrderStatusHistoryResponse.builder()
                .id(h.getId())
                .orderId(h.getOrder().getId())
                .oldStatus(h.getOldStatus())
                .newStatus(h.getNewStatus())
                .changedBy(h.getChangedBy())
                .note(h.getNote())
                .changedAt(h.getChangedAt())
                .build();
    }
}
