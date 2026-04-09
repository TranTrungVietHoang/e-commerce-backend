package com.ecommerce.service;

import com.ecommerce.dto.request.ChatMessageRequest;
import com.ecommerce.dto.response.ChatMessageResponse;
import com.ecommerce.dto.response.ConversationSummaryResponse;
import com.ecommerce.entity.ChatMessage;
import com.ecommerce.entity.User;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.ChatMessageRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    
    // Spring Boot cung cấp sẵn object này khi đã khai báo WebSocketConfig
    // Dùng để đẩy data Real-time xuống Client
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Người gửi không tồn tại"));

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Người nhận không tồn tại"));

        // Không cho phép nhắn tin cho chính mình
        if (senderId.equals(request.getReceiverId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể tự nhắn tin cho bản thân");
        }

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .message(request.getMessage())
                .isRead(false)
                .build();

        if (request.getShopId() != null) {
            message.setShop(shopRepository.findById(request.getShopId()).orElse(null));
        }

        message = chatMessageRepository.save(message);
        ChatMessageResponse response = mapToResponse(message);

        // Phát sóng WebSocket lập tức tới riêng hộp thư của receiver:
        // Cú pháp nội tại của Spring STOMP: /user/{receiverEmail}/queue/chat
        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(),
                "/queue/chat",
                response
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(Long userId1, Long userId2) {
        return chatMessageRepository.findConversation(userId1, userId2)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getRecentConversations(Long userId) {
        List<Long> interactedUserIds = chatMessageRepository.findRecentInteractedUserIds(userId);
        List<ConversationSummaryResponse> summaries = new ArrayList<>();
        
        for (Long targetUserId : interactedUserIds) {
            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null) continue;
            
            // Lấy toàn bộ đoạn chat giữa 2 người
            List<ChatMessage> conversation = chatMessageRepository.findConversation(userId, targetUserId);
            if (conversation.isEmpty()) continue;
            
            ChatMessage lastMessage = conversation.get(conversation.size() - 1);
            
            // Tính số tin nhắn mà partner gửi tới mình chưa xem
            long unreadCount = conversation.stream()
                    .filter(c -> c.getSender().getId().equals(targetUserId) && !c.getIsRead())
                    .count();

            summaries.add(ConversationSummaryResponse.builder()
                    .targetUserId(targetUserId)
                    .targetUserName(targetUser.getFullName())
                    .targetUserAvatar(targetUser.getAvatarUrl())
                    .lastMessage(lastMessage.getMessage())
                    .lastMessageAt(lastMessage.getCreatedAt())
                    .unreadCount(unreadCount)
                    .build());
        }
        
        // Sort: Người mới chat nhất nằm trên cùng
        summaries.sort((a, b) -> b.getLastMessageAt().compareTo(a.getLastMessageAt()));
        return summaries;
    }
    
    @Transactional
    public void markConversationAsRead(Long currentUserId, Long targetUserId) {
        List<ChatMessage> unreadMessages = chatMessageRepository.findConversation(currentUserId, targetUserId)
                .stream()
                .filter(c -> c.getReceiver().getId().equals(currentUserId) && !c.getIsRead())
                .collect(Collectors.toList());
                
        unreadMessages.forEach(c -> c.setIsRead(true));
        chatMessageRepository.saveAll(unreadMessages);
    }

    private ChatMessageResponse mapToResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .shopId(message.getShop() != null ? message.getShop().getId() : null)
                .message(message.getMessage())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
