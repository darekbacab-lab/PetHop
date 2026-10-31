package com.pethop.security.dto;

import com.pethop.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @Pattern(regexp = "^$|^[+()0-9 .-]{7,32}$", message = "El teléfono no tiene un formato válido.") String phone,
        UserRole role) { }
