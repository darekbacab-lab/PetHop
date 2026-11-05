package com.pethop.pet;

import com.pethop.shelter.ShelterProfile;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "pets")
public class Pet {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shelter_id", nullable = false)
    private ShelterProfile shelter;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private PetSpecies species;
    @Column(length = 120) private String breed;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private PetSex sex = PetSex.UNKNOWN;
    @Column(name = "age_value") private Short ageValue;
    @Enumerated(EnumType.STRING) @Column(name = "age_unit", length = 12) private AgeUnit ageUnit;
    @Enumerated(EnumType.STRING) @Column(name = "life_stage", length = 16) private PetLifeStage lifeStage;
    @Enumerated(EnumType.STRING) @Column(length = 12) private PetSize size;
    @Column(precision = 6, scale = 2) private BigDecimal weight;
    @Column(length = 100) private String color;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Column(columnDefinition = "text") private String personality;
    @ElementCollection
    @CollectionTable(name = "pet_personality_traits", joinColumns = @JoinColumn(name = "pet_id"))
    @OrderColumn(name = "position")
    @Column(name = "trait", nullable = false, length = 120)
    private List<String> personalityTraits = new ArrayList<>();
    @Column(name = "health_summary", columnDefinition = "text") private String healthSummary;
    @Column(name = "health_condition", columnDefinition = "text") private String healthCondition;
    @Column(name = "adoption_story", columnDefinition = "text") private String story;
    @Column(columnDefinition = "text") private String background;
    @Column(name = "home_type", length = 180) private String homeType;
    @Column(name = "housing_conditions", columnDefinition = "text") private String conditions;
    @Column(name = "recommended_experience", columnDefinition = "text") private String experience;
    @Column(name = "adoption_requirements", columnDefinition = "text") private String requirements;
    @Column private Boolean sterilized;
    @Column private Boolean vaccinated;
    @Column private Boolean dewormed;
    @Column(length = 180) private String location;
    @Enumerated(EnumType.STRING) @Column(name = "adoption_status", nullable = false, length = 16) private AdoptionStatus adoptionStatus = AdoptionStatus.DRAFT;
    @Enumerated(EnumType.STRING) @Column(name = "moderation_status", nullable = false, length = 20) private ModerationStatus moderationStatus = ModerationStatus.PENDING;
    @Column(name = "moderation_reason", columnDefinition = "text") private String moderationReason;
    @Column(name = "moderated_at") private Instant moderatedAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public ShelterProfile getShelter() { return shelter; }
    public void setShelter(ShelterProfile shelter) { this.shelter = shelter; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PetSpecies getSpecies() { return species; }
    public void setSpecies(PetSpecies species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public PetSex getSex() { return sex; }
    public void setSex(PetSex sex) { this.sex = sex; }
    public Short getAgeValue() { return ageValue; }
    public void setAgeValue(Short ageValue) { this.ageValue = ageValue; }
    public AgeUnit getAgeUnit() { return ageUnit; }
    public void setAgeUnit(AgeUnit ageUnit) { this.ageUnit = ageUnit; }
    public PetLifeStage getLifeStage() { return lifeStage; }
    public void setLifeStage(PetLifeStage lifeStage) { this.lifeStage = lifeStage; }
    public PetSize getSize() { return size; }
    public void setSize(PetSize size) { this.size = size; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }
    public List<String> getPersonalityTraits() { return personalityTraits; }
    public void setPersonalityTraits(List<String> personalityTraits) { this.personalityTraits = personalityTraits == null ? new ArrayList<>() : new ArrayList<>(personalityTraits); }
    public String getHealthSummary() { return healthSummary; }
    public void setHealthSummary(String healthSummary) { this.healthSummary = healthSummary; }
    public String getHealthCondition() { return healthCondition; }
    public void setHealthCondition(String healthCondition) { this.healthCondition = healthCondition; }
    public String getStory() { return story; }
    public void setStory(String story) { this.story = story; }
    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }
    public String getHomeType() { return homeType; }
    public void setHomeType(String homeType) { this.homeType = homeType; }
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public Boolean getSterilized() { return sterilized; }
    public void setSterilized(Boolean sterilized) { this.sterilized = sterilized; }
    public Boolean getVaccinated() { return vaccinated; }
    public void setVaccinated(Boolean vaccinated) { this.vaccinated = vaccinated; }
    public Boolean getDewormed() { return dewormed; }
    public void setDewormed(Boolean dewormed) { this.dewormed = dewormed; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public AdoptionStatus getAdoptionStatus() { return adoptionStatus; }
    public void setAdoptionStatus(AdoptionStatus adoptionStatus) { this.adoptionStatus = adoptionStatus; }
    public ModerationStatus getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(ModerationStatus moderationStatus) { this.moderationStatus = moderationStatus; }
    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
    public Instant getModeratedAt() { return moderatedAt; }
    public void setModeratedAt(Instant moderatedAt) { this.moderatedAt = moderatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
