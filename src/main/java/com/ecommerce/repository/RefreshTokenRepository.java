package com.ecommerce.repository;

import com.ecommerce.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository cho RefreshToken
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Tìm refresh token theo ID của người dùng
    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * Thu hồi tất cả refresh token của user (Dùng khi logout, đổi mật khẩu hoặc lock user)
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(Long userId);
}