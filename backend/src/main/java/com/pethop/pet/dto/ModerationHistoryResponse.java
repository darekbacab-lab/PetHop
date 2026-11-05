package com.pethop.pet.dto;

import com.pethop.pet.PetModerationHistory;
import com.pethop.pet.ModerationAction;
import com.pethop.pet.ModerationStatus;
import java.time.Instant;
import java.util.UUID;

public record ModerationHistoryResponse(UUID id, UUID petId, UUID adminId, String adminName,
        ModerationAction action, ModerationStatus previousStatus, ModerationStatus newStatus,
        String reason, Instant createdAt) {
    public static ModerationHistoryResponse from(PetModerationHistory item) {
        return new ModerationHistoryResponse(item.getId(), item.getPet().getId(), item.getAdmin().getId(),
                item.getAdmin().getFullName(), item.getAction(), item.getPreviousStatus(), item.getNewStatus(),
                item.getReason(), item.getCreatedAt());
    }
}
