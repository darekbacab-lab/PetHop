package com.pethop.pet.dto;

import com.pethop.pet.PetImage;
import java.util.UUID;

public record PetImageResponse(UUID id, String url, String originalFilename, String contentType, long fileSize, boolean primary, short displayOrder) {
    public static PetImageResponse from(PetImage image) {
        return new PetImageResponse(image.getId(), "/api/v1/pet-images/" + image.getId(), image.getOriginalFilename(), image.getContentType(), image.getFileSize(), image.isPrimary(), image.getDisplayOrder());
    }
}
