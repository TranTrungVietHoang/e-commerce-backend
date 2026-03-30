package com.ecommerce.service;

import com.ecommerce.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();
    Category getCategoryById(Integer id);
    Category createCategory(Object request); // Dùng Object để khớp với Impl của bạn
    Category updateCategory(Integer id, Object request);
    void deleteCategory(Integer id);
}