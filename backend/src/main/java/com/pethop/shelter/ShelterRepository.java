package com.pethop.shelter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelterRepository extends JpaRepository<ShelterProfile, UUID> {
    Optional<ShelterProfile> findByUserId(UUID userId);
}
