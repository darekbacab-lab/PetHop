package com.pethop.adoption.dto;
import com.pethop.adoption.Adoption;
import java.time.LocalDate;
import java.util.UUID;
public record AdoptionResponse(UUID id,UUID applicationId,UUID petId,String petName,LocalDate adoptionDate,String agreementNotes) {
    public static AdoptionResponse from(Adoption a){return new AdoptionResponse(a.getId(),a.getApplication().getId(),a.getPet().getId(),a.getPet().getName(),a.getAdoptionDate(),a.getAgreementNotes());}
}
