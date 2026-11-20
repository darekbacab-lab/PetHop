package com.pethop.report.dto;
import com.pethop.report.*; import java.time.Instant; import java.util.UUID;
public record ReportResponse(UUID id,UUID petId,String petName,ReportCategory category,String description,ReportStatus status,Instant createdAt,Instant resolvedAt){public static ReportResponse from(UserReport r){return new ReportResponse(r.getId(),r.getPet().getId(),r.getPet().getName(),r.getCategory(),r.getDescription(),r.getStatus(),r.getCreatedAt(),r.getResolvedAt());}}
