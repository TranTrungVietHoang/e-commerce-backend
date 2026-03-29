package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * JPA Specification builder cho tìm kiếm đa tiêu chí sản phẩm.
 * Mỗi method tạo ra 1 điều kiện lọc, được ghép lại bằng Specification.where().and()
 */
public class ProductSpecification {

    private ProductSpecification() {}

    /** Lọc theo từ khóa (tìm trong tên sản phẩm) */
    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
        };
    }

    /** Lọc theo ID danh mục */
    public static Specification<Product> hasCategoryId(Integer categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return cb.conjunction();
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    /** Lọc theo giá tối thiểu */
    public static Specification<Product> hasMinPrice(BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice);
        };
    }

    /** Lọc theo giá tối đa */
    public static Specification<Product> hasMaxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice);
        };
    }

    /** Lọc theo rating tối thiểu */
    public static Specification<Product> hasMinRating(BigDecimal minRating) {
        return (root, query, cb) -> {
            if (minRating == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("rating"), minRating);
        };
    }

    /** Chỉ lấy sản phẩm đang active */
    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");
    }
}
