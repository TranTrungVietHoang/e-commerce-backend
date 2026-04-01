package com.ecommerce.controller;

import com.ecommerce.dto.request.ChatMessageRequest;
import com.ecommerce.entity.User;
import com.ecommerce.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private final ChatService chatService;

    /**
     * STOMP Controller để nhận tin nhắn qua WebSocket.
     * Cực hay: Nó không dùng HTTP Request (GET/POST), mà kết nối giữ nguyên (Persistent).
     *
     * Client sẽ bắn tin với header Destination: /app/chat.send
     * Spring sẽ tự động gọi hàm này và parse dữ liệu JSON dưới @Payload
     */
    @MessageMapping("/chat.send")
    public void processMessage(@Payload @Valid ChatMessageRequest chatMessageRequest, Authentication authentication) {
        
        // Security Context đã được cấy vào từ lúc CONNECT trong WebSocketConfig !
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            log.error("Hủy thao tác STOMP vì mất xác thực JWT");
            return;
        }

        User sender = (User) authentication.getPrincipal();
        log.info("[WebSocket] STOMP - Bắn tin từ [{}] sang [{}]: {}", 
                 sender.getEmail(), chatMessageRequest.getReceiverId(), chatMessageRequest.getMessage());
                 
        // Xử lý xuống DB và Broadcast
        chatService.sendMessage(sender.getId(), chatMessageRequest);
    }
}
