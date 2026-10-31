package com.pethop.pet;

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

@Entity @Table(name="pet_images")
public class PetImage {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="pet_id", nullable=false) private Pet pet;
    @Column(name="storage_path", nullable=false, length=500) private String storagePath;
    @Column(name="original_filename", nullable=false, length=255) private String originalFilename;
    @Column(name="content_type", nullable=false, length=80) private String contentType;
    @Column(name="file_size", nullable=false) private long fileSize;
    @Column(name="is_primary", nullable=false) private boolean primary;
    @Column(name="display_order", nullable=false) private short displayOrder;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @PrePersist void onCreate(){ createdAt=Instant.now(); }
    public UUID getId(){return id;} public Pet getPet(){return pet;} public void setPet(Pet pet){this.pet=pet;}
    public String getStoragePath(){return storagePath;} public void setStoragePath(String value){storagePath=value;}
    public String getOriginalFilename(){return originalFilename;} public void setOriginalFilename(String value){originalFilename=value;}
    public String getContentType(){return contentType;} public void setContentType(String value){contentType=value;}
    public long getFileSize(){return fileSize;} public void setFileSize(long value){fileSize=value;}
    public boolean isPrimary(){return primary;} public void setPrimary(boolean value){primary=value;}
    public short getDisplayOrder(){return displayOrder;} public void setDisplayOrder(short value){displayOrder=value;}
}
