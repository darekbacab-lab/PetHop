package com.pethop.followup.dto;
import com.pethop.followup.PetCondition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public record FollowUpRequest(@NotNull LocalDate reportDate,@NotNull PetCondition petCondition,@Size(max=3000) String behaviorNotes,@Size(max=3000) String healthNotes,@Size(max=5000) String generalNotes){}
