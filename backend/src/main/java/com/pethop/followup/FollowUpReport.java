package com.pethop.followup;

import com.pethop.adoption.Adoption;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name="follow_up_reports")
public class FollowUpReport {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="adoption_id",nullable=false) private Adoption adoption;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private User createdBy;
    @Column(name="report_date",nullable=false) private LocalDate reportDate;
    @Enumerated(EnumType.STRING) @Column(name="pet_condition",nullable=false,length=32) private PetCondition petCondition;
    @Column(name="behavior_notes",columnDefinition="text") private String behaviorNotes;
    @Column(name="health_notes",columnDefinition="text") private String healthNotes;
    @Column(name="general_notes",columnDefinition="text") private String generalNotes;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @PrePersist void onCreate(){createdAt=updatedAt=Instant.now();}@PreUpdate void onUpdate(){updatedAt=Instant.now();}
    public UUID getId(){return id;} public Adoption getAdoption(){return adoption;} public void setAdoption(Adoption v){adoption=v;}
    public User getCreatedBy(){return createdBy;} public void setCreatedBy(User v){createdBy=v;}
    public LocalDate getReportDate(){return reportDate;} public void setReportDate(LocalDate v){reportDate=v;}
    public PetCondition getPetCondition(){return petCondition;} public void setPetCondition(PetCondition v){petCondition=v;}
    public String getBehaviorNotes(){return behaviorNotes;} public void setBehaviorNotes(String v){behaviorNotes=v;}
    public String getHealthNotes(){return healthNotes;} public void setHealthNotes(String v){healthNotes=v;}
    public String getGeneralNotes(){return generalNotes;} public void setGeneralNotes(String v){generalNotes=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
