package com.pethop.followup.dto;
import com.pethop.followup.FollowUpReport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record FollowUpResponse(UUID id,UUID adoptionId,UUID petId,String petName,UUID createdBy,LocalDate reportDate,String petCondition,String behaviorNotes,String healthNotes,String generalNotes,Instant createdAt,Instant updatedAt,List<FollowUpMediaResponse> media){public static FollowUpResponse from(FollowUpReport r,List<FollowUpMediaResponse> media){return new FollowUpResponse(r.getId(),r.getAdoption().getId(),r.getAdoption().getPet().getId(),r.getAdoption().getPet().getName(),r.getCreatedBy().getId(),r.getReportDate(),r.getPetCondition().name(),r.getBehaviorNotes(),r.getHealthNotes(),r.getGeneralNotes(),r.getCreatedAt(),r.getUpdatedAt(),media);}}
