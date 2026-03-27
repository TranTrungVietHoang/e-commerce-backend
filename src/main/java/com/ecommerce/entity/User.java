package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ánh xạ bảng "users" trong DB.
 * 
 * TRIỂN KHAI UserDetails của Spring Security:
 *   → Spring Security dùng interface này để lấy thông tin user khi authenticate.
 *   → getAuthorities(): convert Set<Role> → Set<GrantedAuthority> để Security biết quyền.
 *   → isAccountNonLocked(): kiểm tra status == ACTIVE.
 * 
 * LƯU Ý QUAN TRỌNG:
 *   - Tên cột "password_hash" trong DB ánh xạ vào field "password" (UserDetails).
 *   - KHÔNG map field "passwordHash" vì sẽ conflict với getPassword() của UserDetails.
 *   - fetch = EAGER cho roles: cần load roles ngay để Security kiểm tra quyền.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // Tên cột DB là "password_hash", nhưng field Java là "password"
    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * Trạng thái tài khoản: ACTIVE / LOCKED
     * LOCKED → isAccountNonLocked() = false → Spring Security chặn login tự động.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_level_id")
    private MembershipLevel membershipLevel;

    /**
     * EAGER fetch: phải load roles ngay khi load User
     * vì Spring Security cần roles để kiểm tra quyền trong filter.
     * 
     * Bảng join: user_roles (user_id, role_id)
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

    // =========================================================
    // UserDetails Implementation (Spring Security)
    // =========================================================

    /**
     * Convert Set<Role> → Collection<GrantedAuthority>.
     * Ví dụ: Role "ROLE_ADMIN" → SimpleGrantedAuthority("ROLE_ADMIN").
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        // Spring Security dùng email làm username
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Tài khoản không có khái niệm hết hạn trong hệ thống này
    }

    /**
     * Kiểm tra tài khoản có bị khóa không.
     * Khi Admin lock user → status = "LOCKED" → return false
     * → Spring Security tự động từ chối đăng nhập với lỗi LockedException.
     */
    @Override
    public boolean isAccountNonLocked() {
        return "ACTIVE".equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }
}
