package com.pethop.adoption.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
public record CreateAdoptionRequest(@NotNull UUID applicationId,@NotNull LocalDate adoptionDate,@Size(max=3000) String agreementNotes) { }
