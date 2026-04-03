package com.ecommerce.dto.response.revenue;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticsResponse implements Serializable {
    private Long totalOrders;
    private Long deliveredOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private List<DailyRevenueData> chartData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenueData implements Serializable {
        private String date; // Format: dd-MM-yyyy, MM-yyyy, hoặc yyyy
        private Long orderCount;
        private BigDecimal revenue;
    }
}
