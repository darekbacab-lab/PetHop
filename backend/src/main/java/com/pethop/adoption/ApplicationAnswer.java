package com.pethop.adoption;

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

@Entity @Table(name="application_answers")
public class ApplicationAnswer {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id",nullable=false) private AdoptionApplication application;
    @Column(name="question_key",nullable=false,length=100) private String questionKey;
    @Column(name="question_text",nullable=false,length=500) private String questionText;
    @Column(name="answer_text",nullable=false,columnDefinition="text") private String answerText;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void onCreate(){createdAt=Instant.now();}
    public void setApplication(AdoptionApplication v){application=v;} public void setQuestionKey(String v){questionKey=v;}
    public void setQuestionText(String v){questionText=v;} public void setAnswerText(String v){answerText=v;}
}
