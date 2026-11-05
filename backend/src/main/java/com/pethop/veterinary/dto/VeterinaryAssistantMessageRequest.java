package com.pethop.veterinary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record VeterinaryAssistantMessageRequest(@NotBlank @Size(max = 2000) String message, UUID petId) {
    public VeterinaryAssistantMessageRequest(String message) { this(message, null); }
}
