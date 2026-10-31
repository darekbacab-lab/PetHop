package com.pethop.pet.dto;
import com.pethop.pet.AdoptionStatus;
import jakarta.validation.constraints.NotNull;
public record UpdatePetStatusRequest(@NotNull AdoptionStatus adoptionStatus) { }
