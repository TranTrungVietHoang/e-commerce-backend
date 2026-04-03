package com.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long shopId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
