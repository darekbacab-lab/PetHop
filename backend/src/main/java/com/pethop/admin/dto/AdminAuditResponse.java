package com.pethop.admin.dto;
import com.pethop.admin.AdminAuditEvent; import java.time.Instant; import java.util.UUID;
public record AdminAuditResponse(UUID id,String adminName,String action,String entityType,String reason,Instant createdAt){public static AdminAuditResponse from(AdminAuditEvent e){return new AdminAuditResponse(e.getId(),e.getAdmin().getFullName(),e.getAction(),e.getEntityType(),e.getReason(),e.getCreatedAt());}}
