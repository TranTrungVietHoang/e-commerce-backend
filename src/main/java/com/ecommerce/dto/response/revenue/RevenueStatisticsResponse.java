package com.ecommerce.dto.response.revenue;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticsResponse {
    private Long totalOrders;
    private Long deliveredOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private List<DailyRevenueData> chartData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenueData {
        private String date;
        private Long orderCount;
        private BigDecimal revenue;
    }
}
