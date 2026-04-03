package com.ecommerce.repository;

import com.ecommerce.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT c FROM ChatMessage c WHERE (c.sender.id = :userId1 AND c.receiver.id = :userId2) " +
           "OR (c.sender.id = :userId2 AND c.receiver.id = :userId1) ORDER BY c.createdAt ASC")
    List<ChatMessage> findConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT DISTINCT CASE WHEN c.sender.id = :userId THEN c.receiver.id ELSE c.sender.id END " +
           "FROM ChatMessage c WHERE c.sender.id = :userId OR c.receiver.id = :userId")
    List<Long> findRecentInteractedUserIds(@Param("userId") Long userId);
    
    long countByReceiverIdAndIsReadFalse(Long receiverId);
}
