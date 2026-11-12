package com.pethop.adoption;
import com.pethop.pet.storage.StoredFile;
import com.pethop.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
@Entity @Table(name="application_documents")
public class ApplicationDocument {
 @Id @UuidGenerator private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id",nullable=false) private AdoptionApplication application;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="uploaded_by",nullable=false) private User uploadedBy;
 @Column(name="document_type",nullable=false,length=40) private String documentType;
 @Column(name="storage_path",nullable=false,length=500) private String storagePath;
 @Column(name="original_filename",nullable=false,length=255) private String originalFilename;
 @Column(name="content_type",nullable=false,length=80) private String contentType;
 @Column(name="file_size",nullable=false) private long fileSize;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @PrePersist void onCreate(){createdAt=Instant.now();}
 public UUID getId(){return id;} public AdoptionApplication getApplication(){return application;} public void setApplication(AdoptionApplication v){application=v;}
 public User getUploadedBy(){return uploadedBy;} public void setUploadedBy(User v){uploadedBy=v;} public String getDocumentType(){return documentType;} public void setDocumentType(String v){documentType=v;}
 public String getStoragePath(){return storagePath;} public void setStoragePath(String v){storagePath=v;} public String getOriginalFilename(){return originalFilename;} public void setOriginalFilename(String v){originalFilename=v;}
 public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;} public long getFileSize(){return fileSize;} public void setFileSize(long v){fileSize=v;} public Instant getCreatedAt(){return createdAt;}
}
