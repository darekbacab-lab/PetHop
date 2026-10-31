package com.pethop.adoption.dto;

import com.pethop.adoption.AdoptionApplication;
import com.pethop.adoption.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record ApplicationReviewResponse(UUID id,UUID petId,String petName,ApplicationStatus status,
        String motivation,String housingType,Boolean hasOtherPets,Boolean hasChildren,String experience,
        String availability,String additionalInformation,String applicantName,String applicantEmail,
        String applicantPhone,Instant submittedAt,Instant reviewedAt) {
    public static ApplicationReviewResponse from(AdoptionApplication a){return new ApplicationReviewResponse(a.getId(),a.getPet().getId(),a.getPet().getName(),a.getStatus(),a.getMotivation(),a.getHousingType(),a.getHasOtherPets(),a.getHasChildren(),a.getExperience(),a.getAvailability(),a.getAdditionalInformation(),a.getAdopter().getFullName(),a.getAdopter().getEmail(),a.getAdopter().getPhone(),a.getSubmittedAt(),a.getReviewedAt());}
}
