package com.ecommerce.repository;

import com.ecommerce.entity.User;
import com.ecommerce.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository cho VerificationToken
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    // Tìm bằng chuỗi token (dùng cho link xác thực hoặc OTP đơn lẻ)
    Optional<VerificationToken> findByToken(String token);

    // Tìm token mới nhất của User theo loại (để lấy mã OTP vừa gửi xong)
    Optional<VerificationToken> findFirstByUserAndTypeOrderByCreatedAtDesc(User user, String type);

    // Tìm token dựa trên Email, chuỗi mã và loại token (Rất quan trọng cho logic verify OTP)
    Optional<VerificationToken> findByUserEmailAndTokenAndType(String email, String token, String type);

    // Kiểm tra xem mã này đã tồn tại và chưa bị sử dụng hay chưa
    boolean existsByTokenAndIsUsedFalse(String token);

    /**
     * Xóa các token cũ của user theo loại để làm sạch database trước khi gửi mã mới
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken v WHERE v.user = :user AND v.type = :type")
    void deleteByUserAndType(User user, String type);
}
