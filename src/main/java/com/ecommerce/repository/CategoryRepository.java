package com.ecommerce.repository;

import com.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho Category
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 1. Lấy tất cả danh mục gốc (parent_id IS NULL)
    List<Category> findByParentIsNull();

    // 2. Tìm danh mục theo Slug (Dùng Optional để tránh lỗi Null)
    Optional<Category> findBySlug(String slug);

    // 3. Tìm tất cả danh mục con bằng ID của cha
    List<Category> findByParentId(Long parentId);

    // 4. Kiểm tra trùng tên danh mục
    boolean existsByName(String name);

    // 5. Kiểm tra duplicate slug
    boolean existsBySlug(String slug);
}
