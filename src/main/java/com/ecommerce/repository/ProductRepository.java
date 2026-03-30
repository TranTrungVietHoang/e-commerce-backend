package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND (p.status IS NULL OR p.status <> :status)")
    Page<Product> findByShopIdAndStatusNot(@Param("shopId") Long shopId, @Param("status") String status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
    List<Product> findAllActiveProducts();

    Optional<Product> findByIdAndShopId(Long id, Long shopId);
    Optional<Product> findBySlug(String slug);

    // 8 sản phẩm mới nhất còn active
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
    List<Product> findTop8ByOrderByCreatedAtDesc(Pageable pageable);

    // 8 sản phẩm bán chạy nhất còn active
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' ORDER BY p.soldCount DESC")
    List<Product> findTop8ByOrderBySoldCountDesc(Pageable pageable);

    // Autocomplete suggestions – tìm theo tên, giới hạn kết quả
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> findSuggestions(@Param("keyword") String keyword, Pageable pageable);
}
