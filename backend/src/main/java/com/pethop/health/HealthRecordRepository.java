package com.pethop.health;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HealthRecordRepository extends JpaRepository<PetHealthRecord,UUID>{List<PetHealthRecord> findAllByPetIdOrderByPerformedAtDesc(UUID petId);List<PetHealthRecord> findAllByPetIdAndNextDueAtGreaterThanEqualOrderByNextDueAtAsc(UUID petId,java.time.LocalDate date);}
