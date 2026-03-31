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
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND)); 
    }

    @Override
    @Transactional
    public Category createCategory(Object request) {
        // Kiểm tra instance trước khi ép kiểu để tránh Runtime Error
        if (!(request instanceof Category)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_ERROR);
        }
        
        Category category = (Category) request;
        
        if (categoryRepository.existsByName(category.getName())) {
            // Thay đổi từ EXCEPTION sang ERROR để khớp với ErrorCode.java của bạn
            throw new AppException(ErrorCode.UNCATEGORIZED_ERROR, "Danh mục đã tồn tại");
        }
        
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Integer id, Object request) {
        Category existing = getCategoryById(id);
        
        if (!(request instanceof Category)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_ERROR);
        }
        
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
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        categoryRepository.deleteById(id);
    }
}