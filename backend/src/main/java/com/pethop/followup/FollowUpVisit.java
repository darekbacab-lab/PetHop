package com.pethop.followup;

import com.pethop.adoption.Adoption;
import com.pethop.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name="follow_up_visits")
public class FollowUpVisit {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="adoption_id",nullable=false) private Adoption adoption;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private User createdBy;
    @Column(name="visit_date",nullable=false) private LocalDate visitDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private VisitStatus status=VisitStatus.PLANNED;
    @Column(length=120) private String outcome;
    @Column(columnDefinition="text") private String notes;
    @Column(name="next_action",columnDefinition="text") private String nextAction;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @PrePersist void onCreate(){createdAt=updatedAt=Instant.now();}@PreUpdate void onUpdate(){updatedAt=Instant.now();}
    public UUID getId(){return id;} public Adoption getAdoption(){return adoption;} public void setAdoption(Adoption v){adoption=v;}
    public User getCreatedBy(){return createdBy;} public void setCreatedBy(User v){createdBy=v;}
    public LocalDate getVisitDate(){return visitDate;} public void setVisitDate(LocalDate v){visitDate=v;}
    public VisitStatus getStatus(){return status;} public void setStatus(VisitStatus v){status=v;}
    public String getOutcome(){return outcome;} public void setOutcome(String v){outcome=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getNextAction(){return nextAction;} public void setNextAction(String v){nextAction=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
