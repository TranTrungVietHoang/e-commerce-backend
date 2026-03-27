package com.ecommerce.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Đọc cấu hình JWT từ application.yml (inject qua @Value).
 * Giá trị thực được lấy từ biến môi trường trong file .env.
 * 
 * CÁC TRƯỜNG:
 *   secret          → Key bí mật để ký và verify JWT (phải đủ mạnh, >= 32 ký tự)
 *   accessExpiration  → Thời gian sống của AccessToken (ms) — mặc định 15 phút
 *   refreshExpiration → Thời gian sống của RefreshToken (ms) — mặc định 7 ngày
 */
@Getter
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.access}")
    private long accessExpiration;

    @Value("${jwt.expiration.refresh}")
    private long refreshExpiration;
}
