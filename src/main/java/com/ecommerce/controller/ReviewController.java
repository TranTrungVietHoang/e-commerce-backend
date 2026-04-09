package com.ecommerce.controller;

import com.ecommerce.dto.request.ReviewRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.ReviewResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponse> response = reviewService.getReviewsByProduct(productId, rating, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/product/{productId}/average")
    public ResponseEntity<ApiResponse<Double>> getAverageRating(@PathVariable Long productId) {
        Double average = reviewService.getAverageRating(productId);
        return ResponseEntity.ok(ApiResponse.success(average));
    }
}
