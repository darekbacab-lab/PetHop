package com.pethop.adoption;
import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument,UUID>{List<ApplicationDocument> findAllByApplicationIdOrderByCreatedAtDesc(UUID applicationId);}
