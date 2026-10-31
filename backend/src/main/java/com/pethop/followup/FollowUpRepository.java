package com.pethop.followup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FollowUpRepository extends JpaRepository<FollowUpReport,UUID>{List<FollowUpReport> findAllByAdoptionIdOrderByReportDateDesc(UUID adoptionId);}
