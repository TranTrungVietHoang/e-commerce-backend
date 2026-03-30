package com.ecommerce.service.impl;

import com.ecommerce.entity.Category;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        // Trả về toàn bộ danh mục để FE tự xử lý cây hoặc hiển thị list
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Bạn có thể thêm CATEGORY_NOT_FOUND vào ErrorCode
    }

    @Override
    @Transactional
    public Category createCategory(Object request) {
        // Giả sử request là Category entity để demo nhanh
        Category category = (Category) request;
        
        if (categoryRepository.existsByName(category.getName())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Nên là CATEGORY_ALREADY_EXISTS
        }
        
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Integer id, Object request) {
        Category existing = getCategoryById(id);
        Category updateData = (Category) request;
        
        existing.setName(updateData.getName());
        existing.setSlug(updateData.getSlug());
        existing.setIconUrl(updateData.getIconUrl());
        existing.setParent(updateData.getParent());
        
        return categoryRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        categoryRepository.deleteById(id);
    }
}