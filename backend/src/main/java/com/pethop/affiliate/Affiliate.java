package com.pethop.affiliate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name = "affiliates")
public class Affiliate {
    @Id @UuidGenerator private UUID id;
    @Column(nullable=false,length=180) private String name;
    @Column(nullable=false,length=40) private String category;
    @Column(columnDefinition="text") private String description;
    @Column(length=180) private String location;
    @Column(name="website_url",length=500) private String websiteUrl;
    @Column(name="discount_code",length=80) private String discountCode;
    @Column(name="discount_text",length=240) private String discountText;
    @Column(nullable=false) private boolean active = true;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @PrePersist void onCreate(){ createdAt=updatedAt=Instant.now(); }
    @PreUpdate void onUpdate(){ updatedAt=Instant.now(); }
    public UUID getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getLocation(){return location;} public void setLocation(String v){location=v;}
    public String getWebsiteUrl(){return websiteUrl;} public void setWebsiteUrl(String v){websiteUrl=v;}
    public String getDiscountCode(){return discountCode;} public void setDiscountCode(String v){discountCode=v;}
    public String getDiscountText(){return discountText;} public void setDiscountText(String v){discountText=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
