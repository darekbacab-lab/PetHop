package com.pethop.adoption;

import com.pethop.pet.Pet;
import com.pethop.shelter.ShelterProfile;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name="adoption_applications")
public class AdoptionApplication {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="pet_id",nullable=false) private Pet pet;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="adopter_id",nullable=false) private User adopter;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shelter_id",nullable=false) private ShelterProfile shelter;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ApplicationStatus status=ApplicationStatus.SUBMITTED;
    @Column(nullable=false,columnDefinition="text") private String motivation;
    @Column(name="housing_type",length=100) private String housingType;
    @Column(name="has_other_pets") private Boolean hasOtherPets;
    @Column(name="has_children") private Boolean hasChildren;
    @Column(columnDefinition="text") private String experience;
    @Column(columnDefinition="text") private String availability;
    @Column(name="additional_information",columnDefinition="text") private String additionalInformation;
    @Column(name="submitted_at",nullable=false) private Instant submittedAt;
    @Column(name="reviewed_at") private Instant reviewedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private User reviewedBy;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @PrePersist void onCreate(){submittedAt=createdAt=updatedAt=Instant.now();}
    @PreUpdate void onUpdate(){updatedAt=Instant.now();}
    public UUID getId(){return id;} public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;}
    public User getAdopter(){return adopter;} public void setAdopter(User v){adopter=v;}
    public ShelterProfile getShelter(){return shelter;} public void setShelter(ShelterProfile v){shelter=v;}
    public ApplicationStatus getStatus(){return status;} public void setStatus(ApplicationStatus v){status=v;}
    public String getMotivation(){return motivation;} public void setMotivation(String v){motivation=v;}
    public String getHousingType(){return housingType;} public void setHousingType(String v){housingType=v;}
    public Boolean getHasOtherPets(){return hasOtherPets;} public void setHasOtherPets(Boolean v){hasOtherPets=v;}
    public Boolean getHasChildren(){return hasChildren;} public void setHasChildren(Boolean v){hasChildren=v;}
    public String getExperience(){return experience;} public void setExperience(String v){experience=v;}
    public String getAvailability(){return availability;} public void setAvailability(String v){availability=v;}
    public String getAdditionalInformation(){return additionalInformation;} public void setAdditionalInformation(String v){additionalInformation=v;}
    public Instant getSubmittedAt(){return submittedAt;} public Instant getReviewedAt(){return reviewedAt;} public void setReviewedAt(Instant v){reviewedAt=v;}
    public User getReviewedBy(){return reviewedBy;} public void setReviewedBy(User v){reviewedBy=v;}
    public Instant getCreatedAt(){return createdAt;}
}
