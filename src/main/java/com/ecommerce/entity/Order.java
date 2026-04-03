package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(exclude = {"customer", "shop", "voucher", "items", "statusHistories"})
@ToString(exclude = {"customer", "shop", "voucher", "items", "statusHistories"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    // Explicit setters to bypass IDE Lombok cache issues
    public void setCustomer(User customer) { this.customer = customer; }
    public void setShop(Shop shop) { this.shop = shop; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "points_used")
    private Integer pointsUsed = 0;

    @Column(nullable = false, length = 30)
    private String status = "PENDING"; // PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED

    @Column(name = "shipping_address", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String shippingAddress;

    @Column(name = "recipient_name", length = 100, nullable = false, columnDefinition = "NVARCHAR(100) DEFAULT 'Chưa xác định'")
    private String recipientName = "Chưa xác định";

    @Column(name = "recipient_phone", length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT '0000000000'")
    private String recipientPhone = "0000000000";

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // COD, SEPAY_TRANSFER

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStatusHistory> statusHistories = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
