package com.pethop.followup;

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

@Entity @Table(name="follow_up_media")
public class FollowUpMedia {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="follow_up_id",nullable=false) private FollowUpReport followUp;
    @Column(name="storage_path",nullable=false,length=500) private String storagePath;
    @Column(name="original_filename",nullable=false,length=255) private String originalFilename;
    @Column(name="content_type",nullable=false,length=80) private String contentType;
    @Column(name="file_size",nullable=false) private long fileSize;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void onCreate(){createdAt=Instant.now();}
    public UUID getId(){return id;} public FollowUpReport getFollowUp(){return followUp;} public void setFollowUp(FollowUpReport v){followUp=v;}
    public String getStoragePath(){return storagePath;} public void setStoragePath(String v){storagePath=v;}
    public String getOriginalFilename(){return originalFilename;} public void setOriginalFilename(String v){originalFilename=v;}
    public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
    public long getFileSize(){return fileSize;} public void setFileSize(long v){fileSize=v;}
}
