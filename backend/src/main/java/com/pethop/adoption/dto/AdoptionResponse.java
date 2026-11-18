package com.pethop.adoption.dto;
import com.pethop.adoption.Adoption;
import java.time.LocalDate;
import java.util.UUID;
public record AdoptionResponse(UUID id,UUID applicationId,UUID petId,String petName,String shelterName,LocalDate adoptionDate,String agreementNotes) {
    public static AdoptionResponse from(Adoption a){return new AdoptionResponse(a.getId(),a.getApplication().getId(),a.getPet().getId(),a.getPet().getName(),a.getPet().getShelter().getOrganizationName(),a.getAdoptionDate(),a.getAgreementNotes());}
}
