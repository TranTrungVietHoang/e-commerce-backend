package com.ecommerce.repository;

import com.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Dùng cho login và UserDetailsService
    Optional<User> findByEmail(String email);

    // Dùng khi đăng ký để kiểm tra trùng
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /**
     * Tìm kiếm user cho Admin dashboard.
     * Tìm theo email HOẶC fullName (case-insensitive), lọc theo status.
     * 
     * @param keyword từ khóa tìm kiếm (null = không lọc)
     * @param status  trạng thái tài khoản (null = hiển thị tất cả)
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> searchUsers(@Param("keyword") String keyword,
                           @Param("status") String status,
                           Pageable pageable);
}
