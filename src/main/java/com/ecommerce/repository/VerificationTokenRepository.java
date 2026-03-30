package com.ecommerce.repository;

import com.ecommerce.entity.User;
import com.ecommerce.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    
    // Tìm token mới nhất của User theo loại (đề phòng có nhiều bản ghi)
    Optional<VerificationToken> findFirstByUserAndTypeOrderByCreatedAtDesc(User user, String type);
    
    @Modifying
    @Transactional
    // Đã xóa @Schema lỗi ở đây
    void deleteByUserAndType(User user, String type);
    
    // Tìm token dựa trên Email của User (Spring Data JPA tự động join bảng User)
    Optional<VerificationToken> findByUserEmailAndTokenAndType(String email, String token, String type);

    // Kiểm tra xem mã này đã tồn tại và chưa sử dụng hay chưa
    boolean existsByTokenAndIsUsedFalse(String token);
}