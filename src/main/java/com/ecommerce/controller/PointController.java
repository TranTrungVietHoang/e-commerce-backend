package com.ecommerce.controller;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.PointHistoryResponse;
import com.ecommerce.dto.response.PointsResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PointsResponse>> getMyPoints(@AuthenticationPrincipal User user) {
        PointsResponse response = pointService.getPointsInfo(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<PointHistoryResponse>>> getPointHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PointHistoryResponse> response = pointService.getPointHistory(user.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
