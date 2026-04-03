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
@RequiredArgsConstructor
@Slf4j
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
                .deliveredOrders(totalOrders) // Vì mình chỉ lấy đơn DELIVERED
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

    @Override
    public RevenueStatisticsResponse getTodayRevenue(Long shopId) {
        log.info("Lấy thống kê doanh thu hôm nay cho shop: {}", shopId);
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<Order> todayOrders = orderRepository.findByShopIdAndStatus(shopId, OrderStatus.DELIVERED)
                .stream()
                .filter(order -> {
                    LocalDateTime created = order.getCreatedAt();
                    return created != null && !created.isBefore(startOfDay) && !created.isAfter(endOfDay);
                })
                .toList();

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
                .build();
    }

    // =========================================================
    // Helper Methods
    // =========================================================

    private List<RevenueStatisticsResponse.DailyRevenueData> groupByPeriod(List<Order> orders, String period) {
        Map<String, RevenueStatisticsResponse.DailyRevenueData> map = new TreeMap<>();
        DateTimeFormatter formatter = getDateTimeFormatter(period);

        orders.forEach(order -> {
            String key = order.getCreatedAt().format(formatter);
            RevenueStatisticsResponse.DailyRevenueData data = map.getOrDefault(key,
                    RevenueStatisticsResponse.DailyRevenueData.builder()
                            .date(key)
                            .orderCount(0L)
                            .revenue(BigDecimal.ZERO)
                            .build());
            
            data.setOrderCount(data.getOrderCount() + 1);
            data.setRevenue(data.getRevenue().add(order.getTotalAmount()));
            map.put(key, data);
        });

        return new ArrayList<>(map.values());
    }

    private DateTimeFormatter getDateTimeFormatter(String period) {
        return switch (period.toUpperCase()) {
            case "MONTH" -> DateTimeFormatter.ofPattern("MM-yyyy");
            case "YEAR" -> DateTimeFormatter.ofPattern("yyyy");
            default -> DateTimeFormatter.ofPattern("dd-MM-yyyy"); // DAY
        };
    }

    private List<TopProductResponse> calculateTopProducts(List<Order> orders, int limit) {
        Map<Long, TopProductResponse> productStats = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                Long productId = item.getProduct().getId();
                TopProductResponse stats = productStats.getOrDefault(productId,
                        TopProductResponse.builder()
                                .productId(productId)
                                .productName(item.getProductName())
                                .soldCount(0L)
                                .totalRevenue(BigDecimal.ZERO)
                                .build());
                
                stats.setSoldCount(stats.getSoldCount() + item.getQuantity());
                stats.setTotalRevenue(stats.getTotalRevenue().add(item.getLineTotal()));
                productStats.put(productId, stats);
            }
        }

        return productStats.values().stream()
                .sorted(Comparator.comparing(TopProductResponse::getSoldCount).reversed())
                .limit(limit)
                .toList();
    }
}
