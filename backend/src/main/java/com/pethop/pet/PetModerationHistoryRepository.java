package com.pethop.pet;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetModerationHistoryRepository extends JpaRepository<PetModerationHistory, UUID> {
    List<PetModerationHistory> findAllByPetIdOrderByCreatedAtDesc(UUID petId);
}
