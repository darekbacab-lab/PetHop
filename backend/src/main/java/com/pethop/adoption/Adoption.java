package com.pethop.adoption;

import com.pethop.pet.Pet;
import com.pethop.shelter.ShelterProfile;
import com.pethop.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name="adoptions")
public class Adoption {
    @Id @UuidGenerator private UUID id;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id",nullable=false,unique=true) private AdoptionApplication application;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="pet_id",nullable=false) private Pet pet;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="adopter_id",nullable=false) private User adopter;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shelter_id",nullable=false) private ShelterProfile shelter;
    @Column(name="adoption_date",nullable=false) private LocalDate adoptionDate;
    @Column(name="agreement_notes",columnDefinition="text") private String agreementNotes;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void onCreate(){createdAt=Instant.now();}
    public UUID getId(){return id;} public AdoptionApplication getApplication(){return application;} public void setApplication(AdoptionApplication v){application=v;}
    public Pet getPet(){return pet;} public void setPet(Pet v){pet=v;} public User getAdopter(){return adopter;} public void setAdopter(User v){adopter=v;}
    public ShelterProfile getShelter(){return shelter;} public void setShelter(ShelterProfile v){shelter=v;}
    public LocalDate getAdoptionDate(){return adoptionDate;} public void setAdoptionDate(LocalDate v){adoptionDate=v;}
    public String getAgreementNotes(){return agreementNotes;} public void setAgreementNotes(String v){agreementNotes=v;}
    public Instant getCreatedAt(){return createdAt;}
}
