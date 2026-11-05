package com.pethop.admin;
import com.pethop.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
@Entity @Table(name = "admin_audit_events")
public class AdminAuditEvent {
 @Id @UuidGenerator private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="admin_id",nullable=false) private User admin;
 @Column(nullable=false,length=60) private String action; @Column(name="entity_type",nullable=false,length=40) private String entityType;
 @Column(name="entity_id",nullable=false) private UUID entityId; @Column(name="previous_state",columnDefinition="text") private String previousState;
 @Column(name="new_state",columnDefinition="text") private String newState; @Column(columnDefinition="text") private String reason;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @PrePersist void onCreate(){createdAt=Instant.now();}
 public UUID getId(){return id;} public User getAdmin(){return admin;} public void setAdmin(User v){admin=v;} public String getAction(){return action;} public void setAction(String v){action=v;} public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;} public UUID getEntityId(){return entityId;} public void setEntityId(UUID v){entityId=v;} public String getPreviousState(){return previousState;} public void setPreviousState(String v){previousState=v;} public String getNewState(){return newState;} public void setNewState(String v){newState=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} public Instant getCreatedAt(){return createdAt;}
}
