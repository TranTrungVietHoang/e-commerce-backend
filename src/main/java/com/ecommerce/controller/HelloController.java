package com.ecommerce.controller;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentMethod;
import com.ecommerce.repository.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class HelloController {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "E-Commerce Backend API");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now());
        response.put("apiDocs", "/swagger-ui.html");
        return response;
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Backend Spring Boot đã sẵn sàng! 🚀");
    }

    @GetMapping("/create-mock-data")
    public Map<String, String> createMockData() {
        try {
            // 1. Tạo Danh Mục (Category) số 1 nếu chưa có
            if (!categoryRepository.existsById(1L)) {
                com.ecommerce.entity.Category category = new com.ecommerce.entity.Category();
                category.setName("Thiết Bị Điện Tử");
                category.setSlug("thiet-bi-dien-tu");
                category.setDescription("Danh mục Công nghệ");
                category.setIconUrl("icon.png");
                categoryRepository.save(category);
            }

            // 2. Lấy 1 tài khoản User bất kỳ ra để gán làm chủ Shop
            com.ecommerce.entity.User seller = userRepository.findAll().stream().findFirst().orElse(null);
            com.ecommerce.entity.Shop shop = null;
            if (seller != null) {
                shop = shopRepository.findById(1L).orElse(null);
                if (shop == null) {
                    shop = new com.ecommerce.entity.Shop();
                    shop.setSeller(seller);
                    shop.setName("Tech Store VN");
                    shop.setDescription("Cửa hàng chính hãng");
                    shop.setRating(new java.math.BigDecimal("5.0"));
                    shop = shopRepository.save(shop);
                }
            }

            // Gắn ảnh mẫu cho sản phẩm đầu tiên nếu chưa có ảnh
            com.ecommerce.entity.Product product = productRepository.findById(1L).orElse(null);
            if (product != null && productImageRepository.findByProductId(1L).isEmpty()) {
                com.ecommerce.entity.ProductImage img = new com.ecommerce.entity.ProductImage();
                img.setProduct(product);
                // Dùng ảnh placeholder ngẫu nhiên đẹp từ Unsplash hoặc Picsum
                img.setImageUrl("https://picsum.photos/seed/keyboard/400/400");
                img.setIsPrimary(true);
                productImageRepository.save(img);
            }

            // 3. Tạo 1 Đơn Hàng Ảo (Trạng thái DELIVERED) để test Review
            if (seller != null && product != null && shop != null && !orderRepository.existsById(1L)) {
                com.ecommerce.entity.Order order = new com.ecommerce.entity.Order();
                order.setCustomer(seller);
                order.setShop(shop);
                order.setSubtotal(product.getBasePrice());
                order.setTotalAmount(product.getBasePrice());
                order.setStatus(OrderStatus.DELIVERED); // Dùng Enum chuẩn
                order.setShippingAddress("Hà Nội");
                order.setPaymentMethod(PaymentMethod.COD); // Dùng Enum chuẩn
                order = orderRepository.save(order);

                com.ecommerce.entity.OrderItem item = new com.ecommerce.entity.OrderItem();
                item.setOrder(order);
                item.setProduct(product);
                item.setQuantity(1);
                item.setUnitPrice(product.getBasePrice());
                orderItemRepository.save(item);
            }

            return Map.of("status", "success", "message", "Đã khởi tạo thành công Danh Mục 1 và Shop 1!");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", "Lỗi: " + e.getMessage());
        }
    }
}
