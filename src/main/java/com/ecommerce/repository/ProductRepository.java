package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND (p.status IS NULL OR p.status <> :status)")
    Page<Product> findByShopIdAndStatusNot(@Param("shopId") Long shopId, @Param("status") String status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.moderationStatus = 'APPROVED' AND p.shop.status = 'APPROVED' ORDER BY p.createdAt DESC")
    List<Product> findAllActiveProducts();

    Optional<Product> findByIdAndShopId(Long id, Long shopId);
    Optional<Product> findBySlug(String slug);
    long countByStatus(String status);

    // 8 sản phẩm mới nhất còn active và đã duyệt
    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.moderationStatus = 'APPROVED' AND p.shop.status = 'APPROVED' ORDER BY p.createdAt DESC")
    List<Product> findActiveAndApprovedOrderByCreatedAtDesc(@Param("status") String status, Pageable pageable);

    // 8 sản phẩm bán chạy nhất còn active và đã duyệt
    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.moderationStatus = 'APPROVED' AND p.shop.status = 'APPROVED' ORDER BY p.soldCount DESC")
    List<Product> findActiveAndApprovedOrderBySoldCountDesc(@Param("status") String status, Pageable pageable);

    // Autocomplete suggestions – tìm theo tên, giới hạn kết quả
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.moderationStatus = 'APPROVED' AND p.shop.status = 'APPROVED' AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> findSuggestions(@Param("keyword") String keyword, Pageable pageable);

    // Danh sách chờ duyệt cho Admin
    Page<Product> findByModerationStatus(String moderationStatus, Pageable pageable);

    // FIX: Find product with pessimistic lock for inventory deduction - tránh race condition
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
