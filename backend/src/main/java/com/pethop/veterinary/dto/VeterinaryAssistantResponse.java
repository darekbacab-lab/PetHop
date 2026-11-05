package com.pethop.veterinary.dto;

import com.pethop.veterinary.VeterinaryUrgency;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VeterinaryAssistantResponse(UUID petId, String petName, VeterinaryUrgency urgency, String summary,
        List<String> reasons, List<String> warningSigns, List<String> guidance, String disclaimer, Instant generatedAt) { }
