package com.pethop.notification;

import com.pethop.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name="notifications")
public class Notification {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(nullable=false,length=40) private String type;
    @Column(nullable=false,length=180) private String title;
    @Column(nullable=false,columnDefinition="text") private String message;
    @Column(name="reference_type",length=60) private String referenceType;
    @Column(name="reference_id") private UUID referenceId;
    @Column(name="read_at") private Instant readAt;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void onCreate(){createdAt=Instant.now();}
    public UUID getId(){return id;} public User getUser(){return user;} public void setUser(User user){this.user=user;}
    public String getType(){return type;} public void setType(String type){this.type=type;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getMessage(){return message;} public void setMessage(String message){this.message=message;}
    public String getReferenceType(){return referenceType;} public void setReferenceType(String value){referenceType=value;}
    public UUID getReferenceId(){return referenceId;} public void setReferenceId(UUID value){referenceId=value;}
    public Instant getReadAt(){return readAt;} public void setReadAt(Instant value){readAt=value;}
    public Instant getCreatedAt(){return createdAt;}
}
