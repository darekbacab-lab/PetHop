package com.pethop.admin.dto;
import com.pethop.admin.AdminAuditEvent; import java.time.Instant; import java.util.UUID;
public record AdminAuditResponse(UUID id,UUID adminId,String adminName,String action,String entityType,UUID entityId,String previousState,String newState,String reason,Instant createdAt){public static AdminAuditResponse from(AdminAuditEvent e){return new AdminAuditResponse(e.getId(),e.getAdmin().getId(),e.getAdmin().getFullName(),e.getAction(),e.getEntityType(),e.getEntityId(),e.getPreviousState(),e.getNewState(),e.getReason(),e.getCreatedAt());}}
