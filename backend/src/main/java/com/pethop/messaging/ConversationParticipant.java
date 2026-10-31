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

@Entity @Table(name="conversation_participants")
public class ConversationParticipant {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="conversation_id",nullable=false) private Conversation conversation;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(name="joined_at",nullable=false,updatable=false) private Instant joinedAt;
    @PrePersist void onCreate(){joinedAt=Instant.now();}
    public UUID getId(){return id;}public Conversation getConversation(){return conversation;}public void setConversation(Conversation v){conversation=v;}
    public User getUser(){return user;}public void setUser(User v){user=v;}public Instant getJoinedAt(){return joinedAt;}
}
