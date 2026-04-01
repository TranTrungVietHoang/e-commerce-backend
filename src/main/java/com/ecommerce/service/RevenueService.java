package com.ecommerce.service;

import com.ecommerce.dto.response.revenue.RevenueStatisticsResponse;

public interface RevenueService {

    /**
     * Lấy thống kê doanh thu của shop theo ngày/tháng/năm
     * @param shopId ID cửa hàng
     * @param period DAY, MONTH, YEAR
     * @return Dữ liệu thống kê
     */
    RevenueStatisticsResponse getShopRevenue(Long shopId, String period);

    /**
     * Lấy thống kê doanh thu toàn nền tảng (Admin only)
     */
    RevenueStatisticsResponse getPlatformRevenue(String period);

    /**
     * Lấy top 10 sản phẩm bán chạy nhất
     */
    java.util.List<com.ecommerce.dto.response.revenue.TopProductResponse> getTopProducts(Long shopId, int limit);

    /**
     * Lấy thống kê doanh thu hôm nay
     */
    RevenueStatisticsResponse getTodayRevenue(Long shopId);
}
