package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @EntityGraph(attributePaths = {"customer", "shop", "voucher", "items", "items.product", "items.variant"})
    Optional<Order> findByIdAndCustomerId(Long orderId, Long customerId);

    @EntityGraph(attributePaths = {"customer", "shop", "items"})
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "items"})
    Page<Order> findByShopId(Long shopId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId AND o.status = :status")
    List<Order> findByCustomerIdAndStatus(@Param("customerId") Long customerId, @Param("status") String status);

    @Query("SELECT o FROM Order o WHERE o.shop.id = :shopId AND o.status = :status")
    List<Order> findByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") String status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.shop.id = :shopId AND o.status = 'DELIVERED'")
    Long countDeliveredOrdersByShop(@Param("shopId") Long shopId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.shop.id = :shopId AND o.status = 'DELIVERED'")
    Optional<java.math.BigDecimal> getTotalRevenueByShop(@Param("shopId") Long shopId);
}
