package com.pethop.adoption.dto;

import com.pethop.adoption.AdoptionApplication;
import com.pethop.adoption.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(UUID id,UUID petId,String petName,UUID shelterId,String shelterName,
        ApplicationStatus status,String motivation,String housingType,Boolean hasOtherPets,Boolean hasChildren,
        String experience,String availability,String additionalInformation,Instant submittedAt,Instant reviewedAt) {
    public static ApplicationResponse from(AdoptionApplication a){return new ApplicationResponse(a.getId(),a.getPet().getId(),a.getPet().getName(),a.getShelter().getId(),a.getShelter().getOrganizationName(),a.getStatus(),a.getMotivation(),a.getHousingType(),a.getHasOtherPets(),a.getHasChildren(),a.getExperience(),a.getAvailability(),a.getAdditionalInformation(),a.getSubmittedAt(),a.getReviewedAt());}
}
