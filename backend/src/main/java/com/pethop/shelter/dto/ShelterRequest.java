package com.pethop.shelter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShelterRequest(@NotBlank @Size(max=180) String organizationName,
        @Size(max=4000) String description, @Size(max=240) String address,
        @Size(max=100) String city, @Size(max=100) String state,
        @Size(max=20) String postalCode,
        @Pattern(regexp="^$|^[+()0-9 .-]{7,32}$") @Size(max=32) String contactPhone) { }
