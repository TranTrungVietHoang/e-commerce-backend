package com.ecommerce.repository;

import com.ecommerce.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    // Lấy toàn bộ lịch sử trạng thái theo đơn hàng, sắp xếp theo thời gian mới nhất trước
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(Long orderId);
}

