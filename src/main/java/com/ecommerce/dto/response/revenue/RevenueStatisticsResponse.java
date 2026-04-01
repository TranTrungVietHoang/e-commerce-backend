package com.ecommerce.dto.response.revenue;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RevenueStatisticsResponse implements Serializable {
    private Long totalOrders;
    private Long deliveredOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private List<DailyRevenueData> chartData;

    @Data
    @Builder
    public static class DailyRevenueData implements Serializable {
        private String date; // Format: YYYY-MM-DD hoặc MM-YYYY
        private Long orderCount;
        private BigDecimal revenue;
    }
}
