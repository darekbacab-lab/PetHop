package com.pethop.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(@NotBlank @Size(max = 200) String token,
        @NotBlank @Size(min = 12, max = 72) String newPassword) { }
