package com.ecommerce.repository;

import com.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId")
    List<OrderItem> findByProductId(@Param("productId") Long productId);

    @Query("SELECT oi FROM OrderItem oi " +
           "WHERE oi.order.customer.id = :userId " +
           "AND oi.product.id = :productId " +
           "AND oi.order.status = com.ecommerce.enums.OrderStatus.DELIVERED")
    List<OrderItem> findDeliveredItemByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
