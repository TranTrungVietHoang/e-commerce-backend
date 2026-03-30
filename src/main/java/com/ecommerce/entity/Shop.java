<<<<<<< Updated upstream
=======
package com.ecommerce.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import com.ecommerce.enums.ShopStatus; // Đã import Enum
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
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Shop ID")
    private Long id;

    @OneToOne
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

    // --- STATUS FIELD ---
    @Enumerated(EnumType.STRING) // Giúp lưu "PENDING" vào DB thay vì số 0
    @Column(length = 20, nullable = false)
    private ShopStatus status = ShopStatus.PENDING; 
    // -------------------------

    @Column(name = "rating")
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
>>>>>>> Stashed changes
