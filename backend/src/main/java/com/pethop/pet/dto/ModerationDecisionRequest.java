package com.pethop.pet.dto;

import com.pethop.pet.ModerationAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerationDecisionRequest(@NotNull ModerationAction action, @Size(max = 2000) String reason) { }
