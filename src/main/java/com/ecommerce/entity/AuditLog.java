package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", length = 100)
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String action; // e.g., "APPROVE_SHOP", "BLOCK_USER"

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String details; // Input parameters or description

    @Column(length = 20)
    private String status; // SUCCESS / FAILURE

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreatedDate
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;
}
