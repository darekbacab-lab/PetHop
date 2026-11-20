package com.pethop.admin.dto;

public record AdminMetricsResponse(long totalPets, long availablePets,
        long openApplications, long formalizedAdoptions, long activeShelters) { }
