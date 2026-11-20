package com.pethop.notification.dto;

import com.pethop.notification.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id,String type,String title,String message,UUID referenceId,Instant readAt,Instant createdAt) {
    public static NotificationResponse from(Notification value){return new NotificationResponse(value.getId(),value.getType(),value.getTitle(),value.getMessage(),value.getReferenceId(),value.getReadAt(),value.getCreatedAt());}
}
