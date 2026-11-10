package com.pethop.admin;
import java.util.UUID; import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.EntityGraph; import org.springframework.data.jpa.repository.JpaRepository;
public interface AdminAuditEventRepository extends JpaRepository<AdminAuditEvent,UUID>{@EntityGraph(attributePaths="admin") Page<AdminAuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);}
