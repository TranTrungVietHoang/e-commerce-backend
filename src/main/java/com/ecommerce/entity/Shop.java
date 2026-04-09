package com.ecommerce.entity;

import com.ecommerce.enums.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho Shop (Cửa hàng của Seller)
 */
@Entity
@Table(name = "shops")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"seller"})
@ToString(exclude = {"seller"})
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Shop ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    @Schema(description = "Chủ shop (User)")
    private User seller;

    @Column(name = "name", nullable = false, length = 255)
    @Schema(description = "Tên shop")
    private String name;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    @Schema(description = "Mô tả shop")
    private String description;

    @Column(name = "logo_url")
    @Schema(description = "URL logo shop")
    private String logoUrl;

    @Column(name = "banner_url")
    @Schema(description = "URL banner shop")
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    @Schema(description = "Trạng thái shop (PENDING, ACTIVE, v.v.)")
    private ShopStatus status = ShopStatus.PENDING;

    @Column(name = "rating", precision = 3, scale = 2)
    @Builder.Default
    @Schema(description = "Đánh giá trung bình")
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Ngày cập nhật")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
