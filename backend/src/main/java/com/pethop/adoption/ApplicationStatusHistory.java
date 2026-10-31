package com.pethop.adoption;

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
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name="application_status_history")
public class ApplicationStatusHistory {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id",nullable=false) private AdoptionApplication application;
    @Enumerated(EnumType.STRING) @Column(name="previous_status",length=20) private ApplicationStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name="new_status",nullable=false,length=20) private ApplicationStatus newStatus;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="changed_by",nullable=false) private User changedBy;
    @Column(columnDefinition="text") private String comment;
    @Column(name="changed_at",nullable=false,updatable=false) private Instant changedAt;
    @PrePersist void onCreate(){changedAt=Instant.now();}
    public UUID getId(){return id;} public AdoptionApplication getApplication(){return application;} public void setApplication(AdoptionApplication v){application=v;}
    public ApplicationStatus getPreviousStatus(){return previousStatus;} public void setPreviousStatus(ApplicationStatus v){previousStatus=v;}
    public ApplicationStatus getNewStatus(){return newStatus;} public void setNewStatus(ApplicationStatus v){newStatus=v;}
    public User getChangedBy(){return changedBy;} public void setChangedBy(User v){changedBy=v;}
    public String getComment(){return comment;} public void setComment(String v){comment=v;}
    public Instant getChangedAt(){return changedAt;}
}
