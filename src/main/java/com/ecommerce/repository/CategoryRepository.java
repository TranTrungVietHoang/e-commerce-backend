<<<<<<< Updated upstream
=======
package com.ecommerce.repository;

import com.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import java.util.Optional;

/**
 * Repository cho Category
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> { // Đổi Long thành Integer

    // 1. Lấy tất cả danh mục gốc (parent_id IS NULL)
    List<Category> findByParentIsNull();

    // 2. Tìm danh mục theo Slug (Dùng Optional để tránh lỗi Null)
    Optional<Category> findBySlug(String slug);

    // 3. Tìm tất cả danh mục con bằng ID của cha
    // Vì id là Integer nên parentId cũng phải là Integer
    List<Category> findByParentId(Integer parentId);

    // 4. Kiểm tra trùng tên danh mục
    boolean existsByName(String name);
}
>>>>>>> Stashed changes
