package com.pethop.adoption.dto;
import com.pethop.adoption.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record UpdateApplicationStatusRequest(@NotNull ApplicationStatus status,@Size(max=2000) String comment) { }
