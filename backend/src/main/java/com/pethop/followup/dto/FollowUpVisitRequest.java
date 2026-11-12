package com.pethop.followup.dto;
import com.pethop.followup.VisitStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public record FollowUpVisitRequest(@NotNull LocalDate visitDate,@NotNull VisitStatus status,@Size(max=120) String outcome,@Size(max=5000) String notes,@Size(max=3000) String nextAction) { }
