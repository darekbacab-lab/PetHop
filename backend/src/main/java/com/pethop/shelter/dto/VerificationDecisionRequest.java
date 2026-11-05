package com.pethop.shelter.dto;
import com.pethop.shelter.VerificationDecision; import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.Size;
public record VerificationDecisionRequest(@NotNull VerificationDecision decision,@Size(max=2000) String reason){}
