package com.pethop.pet.dto;

import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.AgeUnit;
import com.pethop.pet.Pet;
import com.pethop.pet.PetSex;
import com.pethop.pet.PetSize;
import com.pethop.pet.PetSpecies;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PetResponse(UUID id, UUID shelterId, String shelterName, String name, PetSpecies species,
        String breed, PetSex sex, Short ageValue, AgeUnit ageUnit, PetSize size, BigDecimal weight,
        String color, String description, String personality, String healthSummary, boolean sterilized,
        boolean vaccinated, boolean dewormed, String location, AdoptionStatus adoptionStatus,
        Instant publishedAt, List<PetImageResponse> images) {
    public static PetResponse from(Pet pet, List<PetImageResponse> images) {
        return new PetResponse(pet.getId(), pet.getShelter().getId(), pet.getShelter().getOrganizationName(), pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getSex(), pet.getAgeValue(), pet.getAgeUnit(), pet.getSize(), pet.getWeight(), pet.getColor(), pet.getDescription(), pet.getPersonality(), pet.getHealthSummary(), pet.isSterilized(), pet.isVaccinated(), pet.isDewormed(), pet.getLocation(), pet.getAdoptionStatus(), pet.getPublishedAt(), images);
    }
}
