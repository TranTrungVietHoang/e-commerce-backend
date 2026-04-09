package com.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WishlistResponse {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl;
    private LocalDateTime addedAt;
}
