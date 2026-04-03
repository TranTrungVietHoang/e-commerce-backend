package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Tìm tất cả đơn hàng của một khách hàng (phân trang)
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    // Tìm tất cả đơn hàng của một shop (phân trang)
    Page<Order> findByShopId(Long shopId, Pageable pageable);

    // Tìm đơn hàng theo ID và khách hàng (để kiểm tra quyền)
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    // Tìm đơn hàng theo ID và shop (để kiểm tra quyền)
    Optional<Order> findByIdAndShopId(Long id, Long shopId);

    // Tìm đơn theo trạng thái cho shop
    Page<Order> findByShopIdAndStatus(Long shopId, OrderStatus status, Pageable pageable);

    // Tìm đơn theo trạng thái cho khách hàng
    Page<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);
}

