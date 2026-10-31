package com.pethop.messaging.dto;

import jakarta.validation.constraints.NotNull;

public record PinConversationRequest(@NotNull Boolean pinned) {}
