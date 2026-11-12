package com.pethop.followup.dto;
import com.pethop.followup.FollowUpVisit;
import com.pethop.followup.VisitStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
public record FollowUpVisitResponse(UUID id,UUID adoptionId,LocalDate visitDate,VisitStatus status,String outcome,String notes,String nextAction,Instant createdAt,Instant updatedAt){
 public static FollowUpVisitResponse from(FollowUpVisit v){return new FollowUpVisitResponse(v.getId(),v.getAdoption().getId(),v.getVisitDate(),v.getStatus(),v.getOutcome(),v.getNotes(),v.getNextAction(),v.getCreatedAt(),v.getUpdatedAt());}
}
