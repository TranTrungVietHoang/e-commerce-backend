package com.ecommerce.controller;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.revenue.RevenueStatisticsResponse;
import com.ecommerce.dto.response.revenue.TopProductResponse;
import com.ecommerce.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ecommerce.service.UserService;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.exception.BusinessException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/revenue")
@RequiredArgsConstructor
@Slf4j
public class RevenueController {

    private final RevenueService revenueService;
    private final UserService userService;
    private final ShopRepository shopRepository;

    private void verifyShopOwnership(Long requestedShopId, UserDetails userDetails) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        Long sellerShopId = shopRepository.findFirstBySellerId(userId)
                .orElseThrow(() -> new BusinessException("Bạn chưa mở shop"))
                .getId();
        if (!sellerShopId.equals(requestedShopId)) {
            throw new BusinessException("Không có quyền xem doanh thu shop này");
        }
    }

    /**
     * GET /api/v1/revenue/shop/{shopId}?period=DAY
     * Lấy thống kê doanh thu của shop
     * period: DAY, MONTH, YEAR (default: DAY)
     */
    @GetMapping("/shop/{shopId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RevenueStatisticsResponse>> getShopRevenue(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "DAY") String period,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            verifyShopOwnership(shopId, userDetails);
            log.info("Lấy thống kê doanh thu shop: shopId={}, period={}", shopId, period);
            RevenueStatisticsResponse response = revenueService.getShopRevenue(shopId, period);
            return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy thống kê doanh thu: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<RevenueStatisticsResponse> errorResponse = (ApiResponse<RevenueStatisticsResponse>) (Object) ApiResponse
                    .error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/v1/revenue/platform?period=DAY
     * Lấy thống kê doanh thu toàn nền tảng (Admin only)
     */
    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RevenueStatisticsResponse>> getPlatformRevenue(
            @RequestParam(defaultValue = "DAY") String period) {
        try {
            log.info("Lấy thống kê doanh thu nền tảng: period={}", period);
            RevenueStatisticsResponse response = revenueService.getPlatformRevenue(period);
            return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy thống kê nền tảng: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<RevenueStatisticsResponse> errorResponse = (ApiResponse<RevenueStatisticsResponse>) (Object) ApiResponse
                    .error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/v1/revenue/platform/top-products?limit=10
     * Lấy top 10 sản phẩm bán chạy nhất toàn sàn (Admin only)
     */
    @GetMapping("/platform/top-products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getPlatformTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("Lấy top {} sản phẩm của toàn sàn", limit);
            List<TopProductResponse> response = revenueService.getPlatformTopProducts(limit);
            return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy platform top products: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<List<TopProductResponse>> errorResponse = (ApiResponse<List<TopProductResponse>>) (Object) ApiResponse
                    .error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/v1/revenue/shop/{shopId}/top-products?limit=10
     * Lấy top 10 sản phẩm bán chạy nhất của shop
     */
    @GetMapping("/shop/{shopId}/top-products")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            verifyShopOwnership(shopId, userDetails);
            log.info("Lấy top {} sản phẩm của shop: shopId={}", limit, shopId);
            List<TopProductResponse> response = revenueService.getTopProducts(shopId, limit);
            return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy top products: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<List<TopProductResponse>> errorResponse = (ApiResponse<List<TopProductResponse>>) (Object) ApiResponse
                    .error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/v1/revenue/shop/{shopId}/today
     * Lấy thống kê doanh thu hôm nay
     */
    @GetMapping("/shop/{shopId}/today")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RevenueStatisticsResponse>> getTodayRevenue(
            @PathVariable Long shopId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            verifyShopOwnership(shopId, userDetails);
            log.info("Lấy thống kê doanh thu hôm nay: shopId={}", shopId);
            RevenueStatisticsResponse response = revenueService.getTodayRevenue(shopId);
            return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Lỗi lấy doanh thu hôm nay: {}", e.getMessage());
            @SuppressWarnings("unchecked")
            ApiResponse<RevenueStatisticsResponse> errorResponse = (ApiResponse<RevenueStatisticsResponse>) (Object) ApiResponse
                    .error(400, e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    // ==================== PRIVATE METHODS ====================
}
