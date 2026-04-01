package com.ecommerce.controller;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.ChatMessageResponse;
import com.ecommerce.dto.response.ConversationSummaryResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> getRecentConversations(@AuthenticationPrincipal User user) {
        List<ConversationSummaryResponse> response = chatService.getRecentConversations(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history/{targetUserId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getChatHistory(
            @AuthenticationPrincipal User user,
            @PathVariable Long targetUserId) {
        List<ChatMessageResponse> response = chatService.getConversation(user.getId(), targetUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PatchMapping("/history/{targetUserId}/read")
    public ResponseEntity<ApiResponse<Void>> markConversationAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long targetUserId) {
        chatService.markConversationAsRead(user.getId(), targetUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
