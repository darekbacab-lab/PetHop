package com.pethop.adoption.dto;
import com.pethop.adoption.ApplicationDocument; import java.time.Instant; import java.util.UUID;
public record ApplicationDocumentResponse(UUID id,UUID applicationId,String documentType,String originalFilename,String contentType,Instant createdAt){public static ApplicationDocumentResponse from(ApplicationDocument d){return new ApplicationDocumentResponse(d.getId(),d.getApplication().getId(),d.getDocumentType(),d.getOriginalFilename(),d.getContentType(),d.getCreatedAt());}}
