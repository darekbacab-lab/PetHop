package com.pethop.adoption.dto;
import com.pethop.adoption.ApplicationStatus;
import com.pethop.adoption.ApplicationStatusHistory;
import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(UUID id, ApplicationStatus previousStatus, ApplicationStatus newStatus,
        String changedBy, String comment, Instant changedAt) {
    public static StatusHistoryResponse from(ApplicationStatusHistory h) {
        return new StatusHistoryResponse(h.getId(), h.getPreviousStatus(), h.getNewStatus(), h.getChangedBy().getFullName(), h.getComment(), h.getChangedAt());
    }
}
