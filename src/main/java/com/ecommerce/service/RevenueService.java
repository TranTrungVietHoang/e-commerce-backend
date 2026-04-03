package com.ecommerce.service;

import com.ecommerce.dto.response.revenue.RevenueStatisticsResponse;
import com.ecommerce.dto.response.revenue.TopProductResponse;

import java.util.List;

public interface RevenueService {
    RevenueStatisticsResponse getShopRevenue(Long shopId, String period);
    RevenueStatisticsResponse getPlatformRevenue(String period);
    List<TopProductResponse> getTopProducts(Long shopId, int limit);
    List<TopProductResponse> getPlatformTopProducts(int limit);
    RevenueStatisticsResponse getTodayRevenue(Long shopId);
}
