package com.pethop.adoption.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateApplicationRequest(@NotNull UUID petId,@NotBlank @Size(max=3000) String motivation,
        @Size(max=100) String housingType,Boolean hasOtherPets,Boolean hasChildren,
        @Size(max=3000) String experience,@Size(max=2000) String availability,
        @Size(max=3000) String additionalInformation,@Valid @Size(max=30) List<AnswerRequest> answers) { }
