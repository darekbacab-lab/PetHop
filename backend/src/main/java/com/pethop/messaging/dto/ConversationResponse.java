package com.pethop.messaging.dto;
import com.pethop.messaging.Conversation;
import java.time.Instant;
import java.util.UUID;
public record ConversationResponse(UUID id,UUID petId,String petName,String otherParticipant,Instant createdAt,Instant updatedAt){public static ConversationResponse from(Conversation c,String other){return new ConversationResponse(c.getId(),c.getPet().getId(),c.getPet().getName(),other,c.getCreatedAt(),c.getUpdatedAt());}}
