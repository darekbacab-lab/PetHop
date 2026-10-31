package com.pethop.shelter.dto;

import com.pethop.shelter.ShelterProfile;
import com.pethop.shelter.VerificationStatus;
import java.util.UUID;

public record ShelterResponse(UUID id, String organizationName, String description, String address,
        String city, String state, String postalCode, String contactPhone, VerificationStatus verificationStatus) {
    public static ShelterResponse from(ShelterProfile profile) {
        return new ShelterResponse(profile.getId(), profile.getOrganizationName(), profile.getDescription(), profile.getAddress(), profile.getCity(), profile.getState(), profile.getPostalCode(), profile.getContactPhone(), profile.getVerificationStatus());
    }
}
