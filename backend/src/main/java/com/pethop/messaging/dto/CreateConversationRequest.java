package com.pethop.messaging.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateConversationRequest(@NotNull UUID petId,UUID applicationId){}
