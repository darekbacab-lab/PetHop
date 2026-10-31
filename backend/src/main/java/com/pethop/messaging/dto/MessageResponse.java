package com.pethop.messaging.dto;
import com.pethop.messaging.Message;
import java.time.Instant;
import java.util.UUID;
public record MessageResponse(UUID id,UUID senderId,String senderName,String content,Instant readAt,Instant createdAt){public static MessageResponse from(Message m){return new MessageResponse(m.getId(),m.getSender().getId(),m.getSender().getFullName(),m.getContent(),m.getReadAt(),m.getCreatedAt());}}
