package com.pethop.health;

import com.pethop.pet.Pet;
import com.pethop.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name="pet_health_records")
public class PetHealthRecord {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="pet_id",nullable=false) private Pet pet;
    @Enumerated(EnumType.STRING) @Column(name="record_type",nullable=false,length=32) private HealthRecordType recordType;
    @Column(nullable=false,length=180) private String title;
    @Column(columnDefinition="text") private String description;
    @Column(name="performed_at",nullable=false) private LocalDate performedAt;
    @Column(name="next_due_at") private LocalDate nextDueAt;
    @Column(name="veterinarian_name",length=160) private String veterinarianName;
    @Column(name="document_path",length=500) private String documentPath;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private User createdBy;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void onCreate(){createdAt=Instant.now();}
    public UUID getId(){return id;}public Pet getPet(){return pet;}public void setPet(Pet v){pet=v;}public HealthRecordType getRecordType(){return recordType;}public void setRecordType(HealthRecordType v){recordType=v;}
    public String getTitle(){return title;}public void setTitle(String v){title=v;}public String getDescription(){return description;}public void setDescription(String v){description=v;}
    public LocalDate getPerformedAt(){return performedAt;}public void setPerformedAt(LocalDate v){performedAt=v;}public LocalDate getNextDueAt(){return nextDueAt;}public void setNextDueAt(LocalDate v){nextDueAt=v;}
    public String getVeterinarianName(){return veterinarianName;}public void setVeterinarianName(String v){veterinarianName=v;}public String getDocumentPath(){return documentPath;}public void setDocumentPath(String v){documentPath=v;}
    public User getCreatedBy(){return createdBy;}public void setCreatedBy(User v){createdBy=v;}public Instant getCreatedAt(){return createdAt;}
}
