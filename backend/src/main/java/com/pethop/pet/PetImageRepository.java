package com.pethop.pet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetImageRepository extends JpaRepository<PetImage, UUID> {
    List<PetImage> findAllByPetIdOrderByDisplayOrder(UUID petId);
    Optional<PetImage> findByIdAndPetId(UUID id, UUID petId);
    long countByPetId(UUID petId);
    void deleteAllByPetId(UUID petId);
}
