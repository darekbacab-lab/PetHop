package com.pethop.pet;

import com.pethop.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "pet_moderation_history")
public class PetModerationHistory {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "pet_id", nullable = false) private Pet pet;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "admin_id", nullable = false) private User admin;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ModerationAction action;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", nullable = false, length = 20) private ModerationStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 20) private ModerationStatus newStatus;
    @Column(columnDefinition = "text") private String reason;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }
    public User getAdmin() { return admin; }
    public void setAdmin(User admin) { this.admin = admin; }
    public ModerationAction getAction() { return action; }
    public void setAction(ModerationAction action) { this.action = action; }
    public ModerationStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(ModerationStatus value) { previousStatus = value; }
    public ModerationStatus getNewStatus() { return newStatus; }
    public void setNewStatus(ModerationStatus value) { newStatus = value; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
}
