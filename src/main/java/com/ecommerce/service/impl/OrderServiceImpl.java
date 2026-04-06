package com.ecommerce.service.impl;

import com.ecommerce.dto.request.order.CreateOrderRequest;
import com.ecommerce.dto.response.order.OrderDetailResponse;
import com.ecommerce.dto.response.order.OrderListResponse;
import com.ecommerce.dto.response.order.OrderStatusHistoryResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.repository.FlashSaleProductRepository;
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
    private final ShopRepository shopRepository;
    private final VoucherRepository voucherRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FlashSaleProductRepository flashSaleProductRepository;

    @Override
    public OrderDetailResponse createOrder(CreateOrderRequest request, Long customerId) {
        log.info("Tạo đơn hàng mới: customerId={}, shopId={}", customerId, request.getShopId());

        // Lấy thông tin customer
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", customerId));

        // Lấy thông tin shop
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", request.getShopId()));

        // Lấy giỏ hàng của customer
        Cart cart = cartRepository.findByUserId(customerId)
                .orElseThrow(() -> new BusinessException("Giỏ hàng trống"));

        List<CartItem> cartItems = cart.getItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException("Giỏ hàng không có sản phẩm");
        }

        // Kiểm tra stock và tính toán subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            
            if (cartItem.getVariant() != null) {
                // Nếu có variant, check stock variant
                if (cartItem.getVariant().getStock() < cartItem.getQuantity()) {
                    throw new BusinessException("Sản phẩm " + product.getName() + " (variant) không đủ hàng");
                }
            } else {
                // Không variant, check tồn kho product
                if (product.getStockQuantity() < cartItem.getQuantity()) {
                    throw new BusinessException("Sản phẩm " + product.getName() + " không đủ hàng");
                }
            }

            // Kiểm tra tồn kho Flash Sale nếu đang áp dụng giá sale
            flashSaleProductRepository.findActiveByProductId(product.getId()).ifPresent(fsp -> {
                if (cartItem.getUnitPrice().compareTo(fsp.getFlashSalePrice()) == 0) {
                    if (fsp.getSoldCount() + cartItem.getQuantity() > fsp.getSlots()) {
                        throw new BusinessException("Sản phẩm " + product.getName() + " đã hết lượt mua Flash Sale");
                    }
                }
            });

            BigDecimal itemTotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        // Tính discount từ voucher
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher voucher = null;
        if (request.getVoucherId() != null) {
            voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Voucher", request.getVoucherId()));

            // Kiểm tra điều kiện voucher
            if (!isVoucherValid(voucher, subtotal)) {
                throw new BusinessException("Voucher không hợp lệ hoặc không đáp ứng điều kiện");
            }

            // Tính discount
            if ("PERCENT".equals(voucher.getDiscountType())) {
                discountAmount = subtotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
            } else {
                discountAmount = voucher.getDiscountValue();
            }

            // Không vượt quá max discount
            if (voucher.getMaxDiscountValue() != null) {
                discountAmount = discountAmount.min(voucher.getMaxDiscountValue());
            }
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // Tạo Order
        Order order = new Order();
        order.setCustomer(customer);
        order.setShop(shop);
        order.setVoucher(voucher);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);
        order.setPointsUsed(request.getPointsUsed() != null ? request.getPointsUsed() : 0);
        order.setStatus("PENDING");
        order.setShippingAddress(request.getShippingAddress());
        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setPaymentMethod(request.getPaymentMethod());

        Order savedOrder = orderRepository.save(order);
        log.info("Tạo đơn hàng thành công: orderId={}", savedOrder.getId());

        // Tạo OrderItems và trừ kho
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());

            // Cập nhật soldCount cho Flash Sale nếu có
            flashSaleProductRepository.findActiveByProductId(product.getId()).ifPresent(fsp -> {
                if (cartItem.getUnitPrice().compareTo(fsp.getFlashSalePrice()) == 0) {
                    fsp.setSoldCount(fsp.getSoldCount() + cartItem.getQuantity());
                    flashSaleProductRepository.save(fsp);
                }
            });

            orderItemRepository.save(orderItem);
            savedOrder.getItems().add(orderItem);

            // Trừ kho
            if (variant != null) {
                variant.setStock(variant.getStock() - cartItem.getQuantity());
                productVariantRepository.save(variant);
            } else {
                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(product);
            }
        }

        // Lưu trạng thái đầu tiên
        OrderStatusHistory statusHistory = new OrderStatusHistory();
        statusHistory.setOrder(savedOrder);
        statusHistory.setStatus("PENDING");
        statusHistory.setNote("Đơn hàng vừa được tạo");
        orderStatusHistoryRepository.save(statusHistory);
        savedOrder.getStatusHistories().add(statusHistory);

        // Xóa items trong giỏ hàng
        cartItemRepository.deleteAll(cartItems);

        return toDetailResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListResponse> getCustomerOrders(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListResponse> getShopOrders(Long shopId, Pageable pageable) {
        return orderRepository.findByShopId(shopId, pageable)
                .map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getShopOrderDetail(Long orderId, Long shopId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Kiểm tra order có thuộc về shop này không
        if (!order.getShop().getId().equals(shopId)) {
            throw new BusinessException("Đơn hàng này không thuộc về shop bạn");
        }

        return toDetailResponse(order);
    }

    @Override
    public OrderDetailResponse updateOrderStatus(Long orderId, String newStatus, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Kiểm tra seller có quyền update
        if (!order.getShop().getSeller().getId().equals(sellerId)) {
            throw new BusinessException("Bạn không có quyền cập nhật đơn hàng này");
        }

        String oldStatus = order.getStatus();
        
        // Kiểm tra transition status hợp lệ
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new BusinessException("Không thể chuyển từ " + oldStatus + " sang " + newStatus);
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // Lưu lịch sử trạng thái
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(updatedOrder);
        history.setStatus(newStatus);
        history.setNote("Cập nhật từ " + oldStatus + " sang " + newStatus);
        orderStatusHistoryRepository.save(history);

        log.info("Cập nhật trạng thái đơn: orderId={}, status: {} -> {}", orderId, oldStatus, newStatus);

        return toDetailResponse(updatedOrder);
    }

    @Override
    public OrderDetailResponse cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getStatus().matches("PENDING|CONFIRMED")) {
            throw new BusinessException("Chỉ có thể hủy đơn ở trạng thái PENDING hoặc ĐÃ XÁC NHẬN");
        }

        // Hoàn lại kho
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null) {
                item.getVariant().setStock(item.getVariant().getStock() + item.getQuantity());
                productVariantRepository.save(item.getVariant());
            } else {
                item.getProduct().setStockQuantity(item.getProduct().getStockQuantity() + item.getQuantity());
                productRepository.save(item.getProduct());
            }

            // Hoàn lại soldCount cho Flash Sale nếu có
            flashSaleProductRepository.findActiveByProductId(item.getProduct().getId()).ifPresent(fsp -> {
                if (item.getUnitPrice().compareTo(fsp.getFlashSalePrice()) == 0) {
                    fsp.setSoldCount(Math.max(0, fsp.getSoldCount() - item.getQuantity()));
                    flashSaleProductRepository.save(fsp);
                }
            });
        }

        order.setStatus("CANCELLED");
        Order updatedOrder = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(updatedOrder);
        history.setStatus("CANCELLED");
        history.setNote("Đơn hàng bị hủy bởi khách hàng");
        orderStatusHistoryRepository.save(history);

        log.info("Hủy đơn hàng: orderId={}", orderId);

        return toDetailResponse(updatedOrder);
    }

    @Override
    public OrderDetailResponse cancelShopOrder(Long orderId, Long shopId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Kiểm tra shop ownership
        if (!order.getShop().getId().equals(shopId)) {
            throw new BusinessException("Đơn hàng này không thuộc về shop bạn");
        }

        // Chỉ cho phép hủy đơn ở trạng thái PENDING hoặc CONFIRMED
        if (!order.getStatus().matches("PENDING|CONFIRMED")) {
            throw new BusinessException("Chỉ có thể hủy đơn ở trạng thái PENDING hoặc ĐÃ XÁC NHẬN");
        }

        // Hoàn lại kho
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null) {
                item.getVariant().setStock(item.getVariant().getStock() + item.getQuantity());
                productVariantRepository.save(item.getVariant());
            } else {
                item.getProduct().setStockQuantity(item.getProduct().getStockQuantity() + item.getQuantity());
                productRepository.save(item.getProduct());
            }

            // Hoàn lại soldCount cho Flash Sale nếu có
            flashSaleProductRepository.findActiveByProductId(item.getProduct().getId()).ifPresent(fsp -> {
                if (item.getUnitPrice().compareTo(fsp.getFlashSalePrice()) == 0) {
                    fsp.setSoldCount(Math.max(0, fsp.getSoldCount() - item.getQuantity()));
                    flashSaleProductRepository.save(fsp);
                }
            });
        }

        order.setStatus("CANCELLED");
        Order updatedOrder = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(updatedOrder);
        history.setStatus("CANCELLED");
        history.setNote("Đơn hàng bị hủy bởi shop");
        orderStatusHistoryRepository.save(history);

        log.info("Hủy đơn hàng shop: orderId={}, shopId={}", orderId, shopId);

        return toDetailResponse(updatedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId)
                .stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .id(history.getId())
                        .status(history.getStatus())
                        .changedAt(history.getChangedAt())
                        .note(history.getNote())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long checkPurchase(Long userId, Long productId) {
        log.info("Xác thực mua hàng: userId={}, productId={}", userId, productId);
        List<OrderItem> items = orderItemRepository.findDeliveredItemByUserAndProduct(userId, productId);
        if (items != null && !items.isEmpty()) {
            return items.get(0).getId();
        }
        return null;
    }

    // ==================== PRIVATE METHODS ====================

    private OrderDetailResponse toDetailResponse(Order order) {
        List<OrderDetailResponse.OrderItemDTO> items = order.getItems().stream()
                .map(item -> OrderDetailResponse.OrderItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                        .variantAttributes(item.getVariant() != null ? item.getVariant().getAttributes() : null)
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        return OrderDetailResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFullName())
                .shopId(order.getShop().getId())
                .shopName(order.getShop().getName())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .pointsUsed(order.getPointsUsed())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .build();
    }

    private OrderListResponse toListResponse(Order order) {
        return OrderListResponse.builder()
                .id(order.getId())
                .shopName(order.getShop().getName())
                .itemCount(order.getItems().size())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private boolean isVoucherValid(Voucher voucher, BigDecimal orderSubtotal) {
        LocalDateTime now = LocalDateTime.now();

        // Kiểm tra hết hạn
        if (voucher.getExpiresAt() != null && now.isAfter(voucher.getExpiresAt())) {
            return false;
        }

        // Kiểm tra vượt quá số lần sử dụng
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return false;
        }

        // Kiểm tra đơn hàng tối thiểu
        if (voucher.getMinOrderValue() != null && orderSubtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            return false;
        }

        return true;
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // Các transition hợp lệ
        if ("PENDING".equals(currentStatus)) {
            return "CONFIRMED".equals(newStatus) || "CANCELLED".equals(newStatus);
        }
        if ("CONFIRMED".equals(currentStatus)) {
            return "SHIPPING".equals(newStatus) || "CANCELLED".equals(newStatus);
        }
        if ("SHIPPING".equals(currentStatus)) {
            return "DELIVERED".equals(newStatus);
        }

        return false;
    }
}
