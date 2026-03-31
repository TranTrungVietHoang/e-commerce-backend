package com.ecommerce.dto.response.shop;

import com.ecommerce.enums.ShopStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShopResponse {
    private Long id;
    private Long sellerId;
    private String sellerName;
    private String name;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private ShopStatus status;
    private BigDecimal rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
