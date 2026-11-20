package com.pethop.followup.dto;
import com.pethop.followup.FollowUpReport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record FollowUpResponse(UUID id,UUID petId,String petName,LocalDate reportDate,String petCondition,String generalNotes,Instant createdAt,Instant updatedAt,List<FollowUpMediaResponse> media){public static FollowUpResponse from(FollowUpReport r,List<FollowUpMediaResponse> media){return new FollowUpResponse(r.getId(),r.getAdoption().getPet().getId(),r.getAdoption().getPet().getName(),r.getReportDate(),r.getPetCondition().name(),r.getGeneralNotes(),r.getCreatedAt(),r.getUpdatedAt(),media);}}
