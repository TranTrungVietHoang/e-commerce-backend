package com.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long productId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long orderItemId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
