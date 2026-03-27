package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND (p.status IS NULL OR p.status <> :status)")
    Page<Product> findByShopIdAndStatusNot(@Param("shopId") Long shopId, @Param("status") String status, Pageable pageable);
    
    Optional<Product> findByIdAndShopId(Long id, Long shopId);
    Optional<Product> findBySlug(String slug);
}
