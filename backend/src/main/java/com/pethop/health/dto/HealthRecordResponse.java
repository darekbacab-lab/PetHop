package com.pethop.health.dto;
import com.pethop.health.PetHealthRecord;
import com.pethop.health.HealthRecordType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
public record HealthRecordResponse(UUID id,UUID petId,HealthRecordType recordType,String title,String description,LocalDate performedAt,LocalDate nextDueAt,String veterinarianName,Instant createdAt){public static HealthRecordResponse from(PetHealthRecord r){return new HealthRecordResponse(r.getId(),r.getPet().getId(),r.getRecordType(),r.getTitle(),r.getDescription(),r.getPerformedAt(),r.getNextDueAt(),r.getVeterinarianName(),r.getCreatedAt());}}
