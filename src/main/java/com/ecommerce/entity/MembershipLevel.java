package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Ánh xạ bảng "membership_levels".
 * Hạng thành viên dựa trên điểm tích lũy (TB7 quản lý chủ yếu).
 * TB1 chỉ cần dùng khi tạo User (foreign key membership_level_id).
 */
@Entity
@Table(name = "membership_levels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name; // BRONZE, SILVER, GOLD, PLATINUM

    @Column(name = "min_points", nullable = false)
    private Integer minPoints;

    @Column(name = "discount_percent", nullable = false)
    private Double discountPercent;

    @Column(name = "badge_icon")
    private String badgeIcon;
}
