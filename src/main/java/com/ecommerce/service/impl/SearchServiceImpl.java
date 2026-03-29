package com.ecommerce.service.impl;

import com.ecommerce.dto.response.product.ProductResponse;
import com.ecommerce.dto.response.product.SearchSuggestionResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductSpecification;
import com.ecommerce.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(
            String keyword,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            String sort,
            Pageable pageable) {

        // Ghép các điều kiện lọc bằng Specification
        Specification<Product> spec = Specification
                .where(ProductSpecification.isActive())
                .and(ProductSpecification.hasKeyword(keyword))
                .and(ProductSpecification.hasCategoryId(categoryId))
                .and(ProductSpecification.hasMinPrice(minPrice))
                .and(ProductSpecification.hasMaxPrice(maxPrice))
                .and(ProductSpecification.hasMinRating(minRating));

        // Xây dựng sort order từ tham số sort
        Sort sortOrder = buildSort(sort);
        if (sortOrder == null) sortOrder = Sort.unsorted();
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);

        return productRepository.findAll(spec, sortedPageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchSuggestionResponse> getSuggestions(String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return List.of();
        }
        List<Product> results = productRepository.findSuggestions(keyword.trim(), PageRequest.of(0, 8));
        return results.stream().map(p -> {
            String thumbnail = p.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());
            return new SearchSuggestionResponse(p.getId(), p.getName(), p.getSlug(), thumbnail);
        }).collect(Collectors.toList());
    }

    /** Chuyển chuỗi sort thành đối tượng Sort của Spring Data */
    private Sort buildSort(String sort) {
        if (sort == null) return Sort.by("createdAt").descending();
        return switch (sort) {
            case "bestseller"  -> Sort.by("soldCount").descending();
            case "price_asc"   -> Sort.by("basePrice").ascending();
            case "price_desc"  -> Sort.by("basePrice").descending();
            default            -> Sort.by("createdAt").descending(); // newest
        };
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse res = new ProductResponse();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setBasePrice(product.getBasePrice());
        res.setRating(product.getRating());
        res.setSoldCount(product.getSoldCount());
        res.setStatus(product.getStatus());
        res.setCreatedAt(product.getCreatedAt());
        res.setStockQuantity(product.getStockQuantity());
        res.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);

        String primaryImg = product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());
        res.setPrimaryImageUrl(primaryImg);
        return res;
    }
}
