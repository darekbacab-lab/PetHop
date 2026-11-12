package com.pethop.followup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FollowUpVisitRepository extends JpaRepository<FollowUpVisit,UUID>{List<FollowUpVisit> findAllByAdoptionIdOrderByVisitDateDesc(UUID adoptionId);}
