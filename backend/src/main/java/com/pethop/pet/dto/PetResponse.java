package com.pethop.pet.dto;

import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.AgeUnit;
import com.pethop.pet.Pet;
import com.pethop.pet.PetSex;
import com.pethop.pet.PetSize;
import com.pethop.pet.PetSpecies;
import com.pethop.pet.PetLifeStage;
import com.pethop.pet.ModerationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PetResponse(UUID id, UUID shelterId, String shelterName, String name, PetSpecies species,
        String breed, PetSex sex, Short ageValue, AgeUnit ageUnit, PetSize size, BigDecimal weight,
        String color, String description, String personality, String healthSummary, Boolean sterilized,
        Boolean vaccinated, Boolean dewormed, String location, AdoptionStatus adoptionStatus,
        Instant publishedAt, List<PetImageResponse> images, PetLifeStage lifeStage, List<String> personalityTraits,
        String healthCondition, String story, String background, String homeType, String conditions,
        String experience, String requirements, ModerationStatus moderationStatus) {
    public static PetResponse from(Pet pet, List<PetImageResponse> images, List<String> traits) {
        return new PetResponse(pet.getId(), pet.getShelter().getId(), pet.getShelter().getOrganizationName(), pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getSex(), pet.getAgeValue(), pet.getAgeUnit(), pet.getSize(), pet.getWeight(), pet.getColor(), pet.getDescription(), pet.getPersonality(), pet.getHealthSummary(), pet.getSterilized(), pet.getVaccinated(), pet.getDewormed(), pet.getLocation(), pet.getAdoptionStatus(), pet.getPublishedAt(), List.copyOf(images), pet.getLifeStage(), traits == null ? List.of() : List.copyOf(traits), pet.getHealthCondition(), pet.getStory(), pet.getBackground(), pet.getHomeType(), pet.getConditions(), pet.getExperience(), pet.getRequirements(), pet.getModerationStatus());
    }
}
