package com.pethop.shelter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ShelterRepository extends JpaRepository<ShelterProfile, UUID> {
    Optional<ShelterProfile> findByUserId(UUID userId);
    java.util.List<ShelterProfile> findAllByVerificationStatusOrderByCreatedAtAsc(VerificationStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from ShelterProfile s where s.id=:id") Optional<ShelterProfile> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
