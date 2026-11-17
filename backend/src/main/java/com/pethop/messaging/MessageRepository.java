package com.pethop.messaging;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface MessageRepository extends JpaRepository<Message,UUID>{Page<Message> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId,Pageable pageable);Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);long countByConversationIdAndReadAtIsNullAndSender_IdNot(UUID conversationId,UUID senderId);@Modifying @Query("update Message m set m.readAt=CURRENT_TIMESTAMP where m.conversation.id=:conversationId and m.sender.id<>:readerId and m.readAt is null") int markUnreadAsRead(@Param("conversationId") UUID conversationId,@Param("readerId") UUID readerId);}
