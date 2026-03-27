package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByShopIdAndStatusNot(Long shopId, String status, Pageable pageable);
    Optional<Product> findByIdAndShopId(Long id, Long shopId);
    Optional<Product> findBySlug(String slug);
}
