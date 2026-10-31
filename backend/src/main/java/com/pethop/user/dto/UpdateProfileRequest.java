package com.pethop.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@Size(min = 2, max = 160) String fullName,
        @Pattern(regexp = "^$|^[+()0-9 .-]{7,32}$", message = "El teléfono no tiene un formato válido.") String phone) { }
