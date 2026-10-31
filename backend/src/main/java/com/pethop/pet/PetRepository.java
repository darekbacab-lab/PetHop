package com.pethop.pet;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PetRepository extends JpaRepository<Pet, UUID>, JpaSpecificationExecutor<Pet> {
    Optional<Pet> findByIdAndDeletedAtIsNull(UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Pet p where p.id=:id and p.deletedAt is null")
    Optional<Pet> findByIdForUpdate(@Param("id") UUID id);
}
