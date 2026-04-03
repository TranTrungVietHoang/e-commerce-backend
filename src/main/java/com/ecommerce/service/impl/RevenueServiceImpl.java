package com.ecommerce.service.impl;

import com.ecommerce.dto.response.revenue.RevenueStatisticsResponse;
import com.ecommerce.dto.response.revenue.TopProductResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueServiceImpl implements RevenueService {

    private final OrderRepository orderRepository;

    @Override
    public RevenueStatisticsResponse getShopRevenue(Long shopId, String period) {
        log.info("Lấy thống kê doanh thu shop: shopId={}, period={}", shopId, period);

        // Lấy các đơn DELIVERED
        List<Order> orders = orderRepository.findByShopIdAndStatus(shopId, OrderStatus.DELIVERED);

        // Tính toán thống kê
        Long totalOrders = (long) orders.size();
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = totalOrders > 0 
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Nhóm dữ liệu theo period
        List<RevenueStatisticsResponse.DailyRevenueData> chartData = groupByPeriod(orders, period);

        return RevenueStatisticsResponse.builder()
                .totalOrders(totalOrders)
                .deliveredOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(averageOrderValue)
                .chartData(chartData)
                .build();
    }

    @Override
    public RevenueStatisticsResponse getPlatformRevenue(String period) {
        log.info("Lấy thống kê doanh thu toàn sàn: period={}", period);

        // Lấy tất cả đơn DELIVERED trên hệ thống
        List<Order> allOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);

        Long totalOrders = (long) allOrders.size();
        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<RevenueStatisticsResponse.DailyRevenueData> chartData = groupByPeriod(allOrders, period);

        return RevenueStatisticsResponse.builder()
                .totalOrders(totalOrders)
                .deliveredOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(averageOrderValue)
                .chartData(chartData)
                .build();
    }

    @Override
    public List<TopProductResponse> getTopProducts(Long shopId, int limit) {
        log.info("Lấy top {} sản phẩm bán chạy của shop: shopId={}", limit, shopId);
        List<Order> orders = orderRepository.findByShopIdAndStatus(shopId, OrderStatus.DELIVERED);
        return calculateTopProducts(orders, limit);
    }

    @Override
    public List<TopProductResponse> getPlatformTopProducts(int limit) {
        log.info("Lấy top {} sản phẩm bán chạy toàn hệ thống", limit);
        List<Order> allOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);
        return calculateTopProducts(allOrders, limit);
    }

    private List<TopProductResponse> calculateTopProducts(List<Order> orders, int limit) {
        Map<Long, TopProductData> productMap = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                Long productId = item.getProduct().getId();
                String productName = item.getProduct().getName();
                Integer quantity = item.getQuantity();
                BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(quantity));

                productMap.putIfAbsent(productId, new TopProductData(productId, productName));
                TopProductData data = productMap.get(productId);
                data.addSale(quantity, itemTotal);
            }
        }

        return productMap.values().stream()
                .sorted((a, b) -> Long.compare(b.soldCount, a.soldCount))
                .limit(limit)
                .map(data -> TopProductResponse.builder()
                        .productId(data.productId)
                        .productName(data.productName)
                        .soldCount(data.soldCount)
                        .totalRevenue(data.totalRevenue)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public RevenueStatisticsResponse getTodayRevenue(Long shopId) {
        log.info("Lấy thống kê doanh thu hôm nay: shopId={}", shopId);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<Order> todayOrders = orderRepository.findByShopIdAndStatus(shopId, OrderStatus.DELIVERED)
                .stream()
                .filter(order -> {
                    LocalDateTime created = order.getCreatedAt();
                    return !created.isBefore(startOfDay) && !created.isAfter(endOfDay);
                })
                .collect(Collectors.toList());

        Long totalOrders = (long) todayOrders.size();
        BigDecimal totalRevenue = todayOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return RevenueStatisticsResponse.builder()
                .totalOrders(totalOrders)
                .deliveredOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(averageOrderValue)
                .chartData(Collections.singletonList(
                        RevenueStatisticsResponse.DailyRevenueData.builder()
                                .date(today.format(DateTimeFormatter.ISO_DATE))
                                .orderCount(totalOrders)
                                .revenue(totalRevenue)
                                .build()
                ))
                .build();
    }

    // ==================== PRIVATE METHODS ====================

    private List<RevenueStatisticsResponse.DailyRevenueData> groupByPeriod(List<Order> orders, String period) {
        Map<String, RevenueStatisticsResponse.DailyRevenueData> periodMap = new LinkedHashMap<>();

        for (Order order : orders) {
            LocalDateTime createdAt = order.getCreatedAt();
            String key;

            if ("DAY".equalsIgnoreCase(period)) {
                key = createdAt.format(DateTimeFormatter.ISO_DATE); // YYYY-MM-DD
            } else if ("MONTH".equalsIgnoreCase(period)) {
                key = YearMonth.from(createdAt).format(DateTimeFormatter.ofPattern("MM-yyyy")); // MM-YYYY
            } else { // YEAR
                key = String.valueOf(createdAt.getYear()); // YYYY
            }

            periodMap.putIfAbsent(key, RevenueStatisticsResponse.DailyRevenueData.builder()
                    .date(key)
                    .orderCount(0L)
                    .revenue(BigDecimal.ZERO)
                    .build());

            RevenueStatisticsResponse.DailyRevenueData data = periodMap.get(key);
            data.setOrderCount(data.getOrderCount() + 1);
            data.setRevenue(data.getRevenue().add(order.getTotalAmount()));
        }

        return new ArrayList<>(periodMap.values());
    }

    // ==================== INNER CLASS ====================

    private static class TopProductData {
        Long productId;
        String productName;
        Long soldCount = 0L;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        TopProductData(Long productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }

        void addSale(Integer quantity, BigDecimal revenue) {
            this.soldCount += quantity;
            this.totalRevenue = this.totalRevenue.add(revenue);
        }
    }
}
