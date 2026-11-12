package com.pethop.user.dto;

import com.pethop.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull UserRole role) { }
