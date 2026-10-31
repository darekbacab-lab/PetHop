package com.pethop.adoption;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AdoptionRepository extends JpaRepository<Adoption,UUID> {
    Page<Adoption> findAllByAdopterIdOrderByAdoptionDateDesc(UUID adopterId,Pageable pageable);
    Page<Adoption> findAllByShelterIdOrderByAdoptionDateDesc(UUID shelterId,Pageable pageable);
    boolean existsByPetId(UUID petId);
    boolean existsByPetIdAndAdopterId(UUID petId,UUID adopterId);
}
