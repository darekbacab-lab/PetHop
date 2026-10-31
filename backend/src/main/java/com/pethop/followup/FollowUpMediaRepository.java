package com.pethop.followup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FollowUpMediaRepository extends JpaRepository<FollowUpMedia,UUID>{List<FollowUpMedia> findAllByFollowUpId(UUID followUpId);Optional<FollowUpMedia> findByIdAndFollowUpId(UUID id,UUID followUpId);long countByFollowUpId(UUID followUpId);}
