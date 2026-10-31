package com.pethop.messaging;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant,UUID>{boolean existsByConversationIdAndUserId(UUID conversationId,UUID userId);List<ConversationParticipant> findAllByConversationId(UUID conversationId);List<ConversationParticipant> findAllByUserIdOrderByJoinedAtDesc(UUID userId);}
