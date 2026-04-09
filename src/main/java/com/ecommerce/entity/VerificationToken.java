package com.ecommerce.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "VerificationToken ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Schema(description = "User sở hữu token")
    private User user;

    @Column(name = "token", nullable = false, length = 500)
    @Schema(description = "Token string (OTP hoặc verification link)")
    private String token;

    @Column(name = "type", nullable = false, length = 50)
    @Schema(description = "Loại token (PASSWORD_RESET, EMAIL_VERIFICATION, OTP)")
    private String type;

    @Column(name = "expires_at", nullable = false)
    @Schema(description = "Thời hạn token")
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default 
    private Boolean isUsed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Ngày tạo")
    private LocalDateTime createdAt;

    /**
     * Hàm tiện ích kiểm tra xem token đã hết hạn chưa
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    @PrePersist
    protected void onCreate() {
        if (this.isUsed == null) {
            this.isUsed = false;
        }
    }
}
