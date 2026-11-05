package com.pethop.pet;

import com.pethop.common.exception.ApiException;
import com.pethop.common.response.PageResponse;
import com.pethop.adoption.AdoptionRepository;
import com.pethop.adoption.ApplicationRepository;
import com.pethop.adoption.ApplicationStatus;
import com.pethop.pet.dto.PetImageResponse;
import com.pethop.pet.dto.PetRequest;
import com.pethop.pet.dto.PetResponse;
import com.pethop.pet.storage.FileStorage;
import com.pethop.pet.storage.StoredFile;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterProfile;
import com.pethop.shelter.ShelterRepository;
import com.pethop.user.UserRole;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PetService {
    private static final List<ApplicationStatus> ACTIVE_APPLICATION_STATUSES =
            List.of(ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW,
                    ApplicationStatus.APPROVED);
    private final PetRepository pets;
    private final PetImageRepository images;
    private final ShelterRepository shelters;
    private final FileStorage files;
    private final ApplicationRepository applications;
    private final AdoptionRepository adoptions;
    public PetService(PetRepository pets, PetImageRepository images, ShelterRepository shelters, FileStorage files,
            ApplicationRepository applications, AdoptionRepository adoptions) {
        this.pets = pets; this.images = images; this.shelters = shelters; this.files = files;
        this.applications = applications; this.adoptions = adoptions;
    }

    @Transactional(readOnly=true)
    public PageResponse<PetResponse> list(PetSpecies species, PetSex sex, PetSize size, String location,
            String search, String sort, int page, int sizeLimit, SecurityUser principal) {
        Pageable pageable = PageRequest.of(page, sizeLimit, sortOrder(sort));
        UUID shelterId = null;
        AdoptionStatus status = AdoptionStatus.AVAILABLE;
        if (principal != null && (principal.user().getRole() == UserRole.ADMIN || principal.user().getRole() == UserRole.SHELTER)) {
            if (principal.user().getRole() == UserRole.SHELTER) {
                ShelterProfile shelter = shelters.findByUserId(principal.id()).orElse(null);
                if (shelter != null) shelterId = shelter.getId();
                else status = AdoptionStatus.AVAILABLE;
            } else status = null;
        }
        Specification<Pet> spec = PetSpecifications.filters(species, sex, size, location, search, status, shelterId,
                status == AdoptionStatus.AVAILABLE ? ModerationStatus.APPROVED : null);
        return PageResponse.from(pets.findAll(spec, pageable), this::response);
    }

    @Transactional(readOnly=true)
    public PageResponse<PetResponse> listMine(PetSpecies species, PetSex sex, PetSize size, String location,
            String search, String sort, int page, int sizeLimit, SecurityUser principal) {
        Pageable pageable = PageRequest.of(page, sizeLimit, sortOrder(sort));
        UUID shelterId = null;
        if (principal.user().getRole() == UserRole.SHELTER)
            shelterId = shelters.findByUserId(principal.id())
                    .orElseThrow(() -> ApiException.notFound("Perfil de refugio no encontrado.")).getId();
        Specification<Pet> spec = PetSpecifications.filters(species, sex, size, location, search, null, shelterId);
        return PageResponse.from(pets.findAll(spec, pageable), this::response);
    }

    @Transactional(readOnly=true)
    public PetResponse get(UUID id, SecurityUser principal) {
        Pet pet = find(id);
        if ((pet.getAdoptionStatus() != AdoptionStatus.AVAILABLE || pet.getModerationStatus() != ModerationStatus.APPROVED)
                && !owns(pet, principal)) throw ApiException.notFound("Mascota no encontrada.");
        return response(pet);
    }

    @Transactional
    public PetResponse create(SecurityUser principal, PetRequest request) {
        ShelterProfile shelter = ownedShelter(principal);
        Pet pet = new Pet(); pet.setShelter(shelter); apply(pet, request, true);
        pet.setAdoptionStatus(request.adoptionStatus() == null ? AdoptionStatus.DRAFT : requestedEditableStatus(request.adoptionStatus()));
        pet.setModerationStatus(ModerationStatus.PENDING);
        if (pet.getAdoptionStatus() == AdoptionStatus.AVAILABLE) pet.setPublishedAt(Instant.now());
        return response(pets.save(pet));
    }

    @Transactional
    public PetResponse update(UUID id, SecurityUser principal, PetRequest request) {
        Pet pet = ownedPetForUpdate(id, principal);
        ensurePetCanBeEdited(pet);
        apply(pet, request, false);
        pet.setModerationStatus(ModerationStatus.PENDING);
        pet.setModerationReason(null); pet.setModeratedAt(null);
        if (request.adoptionStatus() != null) setStatus(pet, requestedEditableStatus(request.adoptionStatus()));
        return response(pets.save(pet));
    }

    @Transactional
    public PetResponse updateStatus(UUID id, SecurityUser principal, AdoptionStatus status) {
        Pet pet = ownedPetForUpdate(id, principal); setStatus(pet, requestedEditableStatus(status));
        return response(pets.save(pet));
    }

    @Transactional
    public void delete(UUID id, SecurityUser principal) {
        Pet pet = ownedPetForUpdate(id, principal);
        ensurePetCanBeEdited(pet);
        if (applications.existsByPetIdAndStatusIn(pet.getId(), ACTIVE_APPLICATION_STATUSES))
            throw ApiException.conflict("La mascota tiene solicitudes de adopción activas.");
        if (adoptions.existsByPetId(pet.getId()))
            throw ApiException.conflict("La mascota ya tiene una adopción registrada.");
        pet.setDeletedAt(Instant.now()); pets.save(pet);
    }

    @Transactional
    public PetImageResponse addImage(UUID id, SecurityUser principal, MultipartFile file) throws IOException {
        Pet pet = ownedPet(id, principal);
        long count = images.countByPetId(id);
        if (count >= 6) throw ApiException.badRequest("Cada mascota puede tener hasta 6 fotografías.");
        StoredFile stored = files.store(file);
        PetImage image = new PetImage(); image.setPet(pet); image.setStoragePath(stored.path());
        image.setOriginalFilename(stored.originalFilename()); image.setContentType(stored.contentType()); image.setFileSize(stored.size());
        image.setDisplayOrder((short) count); image.setPrimary(count == 0);
        try {
            return PetImageResponse.from(images.save(image));
        } catch (RuntimeException failure) {
            try { files.delete(stored.path()); }
            catch (IOException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            throw failure;
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public void deleteImage(UUID petId, UUID imageId, SecurityUser principal) throws IOException {
        ownedPet(petId, principal);
        PetImage image = images.findByIdAndPetId(imageId, petId).orElseThrow(() -> ApiException.notFound("Imagen no encontrada."));
        String storedPath = image.getStoragePath();
        images.delete(image);
        files.delete(storedPath);
    }

    @Transactional(readOnly=true)
    public PetImage image(UUID imageId, SecurityUser principal) {
        PetImage image = images.findById(imageId).orElseThrow(() -> ApiException.notFound("Imagen no encontrada."));
        if ((image.getPet().getAdoptionStatus() != AdoptionStatus.AVAILABLE || image.getPet().getModerationStatus() != ModerationStatus.APPROVED)
                && !owns(image.getPet(), principal)) throw ApiException.notFound("Imagen no encontrada.");
        return image;
    }

    @Transactional(readOnly=true)
    public List<PetImageResponse> images(UUID petId, SecurityUser principal) {
        Pet pet = find(petId);
        if ((pet.getAdoptionStatus() != AdoptionStatus.AVAILABLE || pet.getModerationStatus() != ModerationStatus.APPROVED)
                && !owns(pet, principal)) throw ApiException.notFound("Mascota no encontrada.");
        return images.findAllByPetIdOrderByDisplayOrder(petId).stream().map(PetImageResponse::from).toList();
    }

    private Pet find(UUID id) { return pets.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> ApiException.notFound("Mascota no encontrada.")); }
    private ShelterProfile ownedShelter(SecurityUser principal) {
        if (principal.user().getRole() != UserRole.SHELTER) throw ApiException.forbidden("Solo refugios pueden administrar mascotas.");
        return shelters.findByUserId(principal.id()).orElseThrow(() -> ApiException.badRequest("Primero registra el perfil del refugio."));
    }
    private Pet ownedPet(UUID id, SecurityUser principal) {
        Pet pet = find(id);
        if (principal.user().getRole() == UserRole.ADMIN) return pet;
        ShelterProfile shelter = ownedShelter(principal);
        if (!pet.getShelter().getId().equals(shelter.getId())) throw ApiException.notFound("Mascota no encontrada.");
        return pet;
    }
    private Pet ownedPetForUpdate(UUID id, SecurityUser principal) {
        Pet pet = pets.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Mascota no encontrada."));
        if (principal.user().getRole() == UserRole.ADMIN) return pet;
        ShelterProfile shelter = ownedShelter(principal);
        if (!pet.getShelter().getId().equals(shelter.getId())) throw ApiException.notFound("Mascota no encontrada.");
        return pet;
    }
    private boolean owns(Pet pet, SecurityUser principal) {
        if (principal == null) return false;
        if (principal.user().getRole() == UserRole.ADMIN) return true;
        if (principal.user().getRole() != UserRole.SHELTER) return false;
        return shelters.findByUserId(principal.id()).map(shelter -> shelter.getId().equals(pet.getShelter().getId())).orElse(false);
    }
    private AdoptionStatus requestedEditableStatus(AdoptionStatus status) {
        if (status == AdoptionStatus.DRAFT || status == AdoptionStatus.AVAILABLE || status == AdoptionStatus.UNAVAILABLE) return status;
        throw ApiException.badRequest("El estado solicitado solo puede ser borrador, disponible o no disponible.");
    }
    private void setStatus(Pet pet, AdoptionStatus status) {
        if (pet.getAdoptionStatus() == AdoptionStatus.IN_PROCESS || pet.getAdoptionStatus() == AdoptionStatus.ADOPTED)
            throw ApiException.conflict("La mascota ya está comprometida con una adopción.");
        if (status == AdoptionStatus.AVAILABLE
                && applications.existsByPetIdAndStatusIn(pet.getId(), ACTIVE_APPLICATION_STATUSES))
            throw ApiException.conflict("La mascota tiene una solicitud de adopción activa.");
        pet.setAdoptionStatus(status);
        if (status == AdoptionStatus.AVAILABLE && pet.getPublishedAt() == null) pet.setPublishedAt(Instant.now());
    }
    private void ensurePetCanBeEdited(Pet pet) {
        if (pet.getAdoptionStatus() == AdoptionStatus.IN_PROCESS || pet.getAdoptionStatus() == AdoptionStatus.ADOPTED)
            throw ApiException.conflict("La mascota ya está comprometida con una adopción.");
    }
    private void apply(Pet pet, PetRequest request, boolean creating) {
        pet.setName(request.name().trim()); pet.setSpecies(request.species()); pet.setBreed(request.breed());
        pet.setSex(request.sex()); pet.setAgeValue(request.ageValue()); pet.setAgeUnit(request.ageUnit()); pet.setSize(request.size());
        pet.setLifeStage(request.lifeStage());
        pet.setWeight(request.weight()); pet.setColor(request.color()); pet.setDescription(request.description().trim());
        List<String> traits = request.personalityTraits() == null ? parseTraits(request.personality()) : request.personalityTraits();
        pet.setPersonalityTraits(traits);
        pet.setPersonality(traits.isEmpty() ? request.personality() : String.join(", ", traits));
        pet.setHealthSummary(request.healthSummary()); pet.setHealthCondition(request.healthCondition());
        pet.setStory(request.story()); pet.setBackground(request.background()); pet.setHomeType(request.homeType());
        pet.setConditions(request.conditions()); pet.setExperience(request.experience()); pet.setRequirements(request.requirements());
        pet.setSterilized(request.sterilized());
        pet.setVaccinated(request.vaccinated()); pet.setDewormed(request.dewormed()); pet.setLocation(request.location());
    }
    private PetResponse response(Pet pet) {
        List<PetImageResponse> photoList = images.findAllByPetIdOrderByDisplayOrder(pet.getId()).stream().map(PetImageResponse::from).toList();
        return PetResponse.from(pet, photoList, pet.getPersonalityTraits().isEmpty() ? parseTraits(pet.getPersonality()) : List.copyOf(pet.getPersonalityTraits()));
    }
    private List<String> parseTraits(String legacyValue) {
        if (legacyValue == null || legacyValue.isBlank() || legacyValue.equalsIgnoreCase("true") || legacyValue.equalsIgnoreCase("false")) return List.of();
        return Arrays.stream(legacyValue.split(",")).map(String::trim).filter(value -> !value.isBlank()).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    private Sort sortOrder(String sort) {
        return switch (sort == null ? "recommended" : sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "publishedAt");
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt");
            default -> Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt");
        };
    }
}
