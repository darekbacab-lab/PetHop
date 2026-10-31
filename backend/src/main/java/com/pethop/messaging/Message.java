package com.pethop.messaging;

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

@Entity @Table(name="messages")
public class Message {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="conversation_id",nullable=false) private Conversation conversation;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sender_id",nullable=false) private User sender;
    @Column(nullable=false,columnDefinition="text") private String content;
    @Column(name="read_at") private Instant readAt;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void onCreate(){createdAt=Instant.now();}
    public UUID getId(){return id;}public Conversation getConversation(){return conversation;}public void setConversation(Conversation v){conversation=v;}
    public User getSender(){return sender;}public void setSender(User v){sender=v;}public String getContent(){return content;}public void setContent(String v){content=v;}
    public Instant getReadAt(){return readAt;}public void setReadAt(Instant v){readAt=v;}public Instant getCreatedAt(){return createdAt;}
}
