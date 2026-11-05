package com.pethop.veterinary.dto;

import com.pethop.pet.AgeUnit;
import com.pethop.pet.PetLifeStage;
import com.pethop.pet.PetSex;
import com.pethop.pet.PetSize;
import com.pethop.pet.PetSpecies;
import java.math.BigDecimal;
import java.util.UUID;

public record VeterinaryPetContextResponse(UUID petId, String name, PetSpecies species, String breed, PetSex sex,
        Short ageValue, AgeUnit ageUnit, PetLifeStage lifeStage, PetSize size, BigDecimal weight, String color,
        String healthSummary, String healthCondition, Boolean sterilized, Boolean vaccinated, Boolean dewormed,
        String conditions, String requirements) { }
