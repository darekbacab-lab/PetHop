package com.pethop.adoption;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ApplicationRepository extends JpaRepository<AdoptionApplication,UUID> {
    boolean existsByAdopterIdAndPetIdAndStatusIn(UUID adopterId,UUID petId,List<ApplicationStatus> statuses);
    Page<AdoptionApplication> findAllByAdopterIdOrderByCreatedAtDesc(UUID adopterId,Pageable pageable);
    Page<AdoptionApplication> findAllByShelterIdOrderByCreatedAtDesc(UUID shelterId,Pageable pageable);
    Optional<AdoptionApplication> findByIdAndAdopterId(UUID id,UUID adopterId);
    Optional<AdoptionApplication> findByIdAndShelterId(UUID id,UUID shelterId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from AdoptionApplication a where a.id=:id")
    Optional<AdoptionApplication> findByIdForUpdate(@Param("id") UUID id);
}
