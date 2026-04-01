package com.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ConversationSummaryResponse {
    private Long targetUserId;
    private String targetUserName;
    private String targetUserAvatar;
    private Long shopId;
    private String shopName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
}
