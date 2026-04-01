package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
@Data
@EqualsAndHashCode(exclude = {"order"})
@ToString(exclude = {"order"})
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
