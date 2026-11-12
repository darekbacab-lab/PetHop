package com.pethop.admin.dto;

public record AdminMetricsResponse(long totalPets, long pendingModeration, long availablePets,
        long totalApplications, long openApplications, long formalizedAdoptions,
        long pendingShelters, long activeAdopters, long activeShelters) { }
