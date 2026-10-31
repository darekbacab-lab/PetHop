package com.pethop.pet.dto;

import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.AgeUnit;
import com.pethop.pet.PetSex;
import com.pethop.pet.PetSize;
import com.pethop.pet.PetSpecies;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PetRequest(@NotBlank @Size(max=100) String name, @NotNull PetSpecies species,
        @Size(max=120) String breed, @NotNull PetSex sex, @PositiveOrZero Short ageValue,
        AgeUnit ageUnit, PetSize size, @DecimalMin("0.01") BigDecimal weight,
        @Size(max=100) String color, @NotBlank @Size(max=5000) String description,
        @Size(max=5000) String personality, @Size(max=5000) String healthSummary,
        boolean sterilized, boolean vaccinated, boolean dewormed, @Size(max=180) String location,
        AdoptionStatus adoptionStatus) { }
