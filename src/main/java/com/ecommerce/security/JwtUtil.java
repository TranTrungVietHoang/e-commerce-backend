package com.ecommerce.security;

import com.ecommerce.config.JwtConfig;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Tiện ích JWT — xử lý toàn bộ vòng đời của Token.
 * 
 * THƯ VIỆN: jjwt (io.jsonwebtoken) v0.11.5 đã có trong pom.xml
 * THUẬT TOÁN KÝ: HMAC-SHA256 (HS256) — đối xứng, nhanh, phù hợp monolith
 * 
 * FLOW:
 *   Login: generateAccessToken() + generateRefreshToken()
 *   Request: extractUsername() → loadUser → validateToken()
 *   Refresh: validateToken(refreshToken) → generateAccessToken()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    // =========================================================
    // GENERATE TOKEN
    // =========================================================

    /**
     * Tạo Access Token cho user đã đăng nhập.
     * Claims chứa thêm roles để SecurityConfig kiểm tra quyền.
     * Hết hạn: 15 phút (cài trong application.yml)
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Nhúng danh sách roles vào payload của token
        extraClaims.put("roles", userDetails.getAuthorities()
                .stream()
                .map(Object::toString)
                .toList());

        return buildToken(extraClaims, userDetails.getUsername(), jwtConfig.getAccessExpiration());
    }

    /**
     * Tạo Refresh Token — chỉ chứa subject (email), không có roles.
     * Hết hạn: 7 ngày (cài trong application.yml)
     * Lưu vào DB để có thể revoke (thu hồi) khi logout hoặc lock user.
     */
    public String generateRefreshToken(String username) {
        return buildToken(new HashMap<>(), username, jwtConfig.getRefreshExpiration());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)                                      // email của user
                .setIssuedAt(new Date(System.currentTimeMillis()))        // thời điểm tạo
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // hết hạn
                .signWith(getSignKey(), SignatureAlgorithm.HS256)          // ký bằng secret
                .compact();
    }

    // =========================================================
    // VALIDATE & EXTRACT
    // =========================================================

    /**
     * Kiểm tra token có hợp lệ không (cả chữ ký + hạn dùng).
     * 
     * @return true nếu hợp lệ
     * @throws AppException nếu token sai hoặc hết hạn
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token đã hết hạn: {}", e.getMessage());
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT token không hợp lệ: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * Lấy username (email) từ token mà không cần validate.
     * Dùng trong JwtAuthenticationFilter trước khi validate.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Kiểm tra token có hợp lệ và thuộc về user này không.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // =========================================================
    // KEY
    // =========================================================

    /**
     * Tạo signing key từ secret string (base64).
     * Secret phải >= 256 bits (32 bytes) cho HS256.
     */
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
