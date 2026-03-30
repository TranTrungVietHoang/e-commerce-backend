package com.ecommerce.controller;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.product.ProductResponse;
import com.ecommerce.dto.response.product.SearchSuggestionResponse;
import com.ecommerce.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * GET /api/search
     * Tìm kiếm đa tiêu chí với phân trang.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Page<ProductResponse> result = searchService.search(
                keyword, categoryId, minPrice, maxPrice, minRating, sort,
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/search/suggestions?q=...
     * Autocomplete – trả tối đa 8 gợi ý (từ khóa tối thiểu 2 ký tự).
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<SearchSuggestionResponse>>> suggestions(
            @RequestParam String q) {
        List<SearchSuggestionResponse> suggestions = searchService.getSuggestions(q);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }
}
