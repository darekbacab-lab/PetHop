package com.pethop.health;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HealthRecordRepository extends JpaRepository<PetHealthRecord,UUID>{List<PetHealthRecord> findAllByPetIdOrderByPerformedAtDesc(UUID petId);List<PetHealthRecord> findAllByPetIdAndNextDueAtBetweenOrderByNextDueAtAsc(UUID petId,java.time.LocalDate from,java.time.LocalDate to);List<PetHealthRecord> findAllByNextDueAtBetween(java.time.LocalDate from,java.time.LocalDate to);}
