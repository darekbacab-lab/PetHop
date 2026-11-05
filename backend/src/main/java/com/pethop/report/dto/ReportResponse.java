package com.pethop.report.dto;
import com.pethop.report.*; import java.time.Instant; import java.util.UUID;
public record ReportResponse(UUID id,UUID reporterId,UUID petId,String petName,ReportCategory category,String description,ReportStatus status,String resolutionDetails,Instant createdAt,Instant resolvedAt){public static ReportResponse from(UserReport r){return new ReportResponse(r.getId(),r.getReporter().getId(),r.getPet().getId(),r.getPet().getName(),r.getCategory(),r.getDescription(),r.getStatus(),r.getResolutionDetails(),r.getCreatedAt(),r.getResolvedAt());}}
