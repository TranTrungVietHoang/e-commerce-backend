package com.ecommerce.service;

import com.ecommerce.dto.response.product.ProductResponse;
import com.ecommerce.dto.response.product.SearchSuggestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface SearchService {

    /**
     * Tìm kiếm sản phẩm đa tiêu chí.
     * sort: newest | bestseller | price_asc | price_desc
     */
    Page<ProductResponse> search(
            String keyword,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            String sort,
            Pageable pageable
    );

    /** Autocomplete – trả tối đa 8 gợi ý */
    List<SearchSuggestionResponse> getSuggestions(String keyword);
}
