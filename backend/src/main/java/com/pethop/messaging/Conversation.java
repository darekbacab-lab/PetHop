package com.pethop.messaging;

import com.pethop.pet.Pet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name="conversations")
public class Conversation {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="pet_id") private Pet pet;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @PrePersist void onCreate(){createdAt=updatedAt=Instant.now();}@PreUpdate void onUpdate(){updatedAt=Instant.now();}
    public UUID getId(){return id;}
    public Pet getPet(){return pet;}public void setPet(Pet v){pet=v;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}public void touch(){updatedAt=Instant.now();}
}
