package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Ánh xạ bảng "roles" trong DB.
 * 
 * 3 roles mặc định (đã seed sẵn trong init.sql):
 *   ID=1 → ROLE_CUSTOMER
 *   ID=2 → ROLE_SELLER
 *   ID=3 → ROLE_ADMIN
 * 
 * Spring Security yêu cầu tên role bắt đầu bằng "ROLE_"
 * khi dùng hasRole("ADMIN") → tự thêm prefix "ROLE_".
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // "ROLE_CUSTOMER", "ROLE_SELLER", "ROLE_ADMIN"
}
