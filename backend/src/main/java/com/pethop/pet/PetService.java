package com.pethop.pet;

import com.pethop.common.exception.ApiException;
import com.pethop.common.response.PageResponse;
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
    private final PetRepository pets;
    private final PetImageRepository images;
    private final ShelterRepository shelters;
    private final FileStorage files;
    public PetService(PetRepository pets, PetImageRepository images, ShelterRepository shelters, FileStorage files) {
        this.pets = pets; this.images = images; this.shelters = shelters; this.files = files;
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
        Specification<Pet> spec = PetSpecifications.filters(species, sex, size, location, search, status, shelterId);
        return PageResponse.from(pets.findAll(spec, pageable), this::response);
    }

    @Transactional(readOnly=true)
    public PetResponse get(UUID id, SecurityUser principal) {
        Pet pet = find(id);
        if (pet.getAdoptionStatus() != AdoptionStatus.AVAILABLE && !owns(pet, principal)) throw ApiException.notFound("Mascota no encontrada.");
        return response(pet);
    }

    @Transactional
    public PetResponse create(SecurityUser principal, PetRequest request) {
        ShelterProfile shelter = ownedShelter(principal);
        Pet pet = new Pet(); pet.setShelter(shelter); apply(pet, request, true);
        pet.setAdoptionStatus(request.adoptionStatus() == null ? AdoptionStatus.DRAFT : requestedEditableStatus(request.adoptionStatus()));
        if (pet.getAdoptionStatus() == AdoptionStatus.AVAILABLE) pet.setPublishedAt(Instant.now());
        return response(pets.save(pet));
    }

    @Transactional
    public PetResponse update(UUID id, SecurityUser principal, PetRequest request) {
        Pet pet = ownedPet(id, principal); apply(pet, request, false);
        if (request.adoptionStatus() != null) setStatus(pet, requestedEditableStatus(request.adoptionStatus()));
        return response(pets.save(pet));
    }

    @Transactional
    public PetResponse updateStatus(UUID id, SecurityUser principal, AdoptionStatus status) {
        Pet pet = ownedPet(id, principal); setStatus(pet, requestedEditableStatus(status));
        return response(pets.save(pet));
    }

    @Transactional
    public void delete(UUID id, SecurityUser principal) { Pet pet = ownedPet(id, principal); pet.setDeletedAt(Instant.now()); pets.save(pet); }

    @Transactional
    public PetImageResponse addImage(UUID id, SecurityUser principal, MultipartFile file) throws IOException {
        Pet pet = ownedPet(id, principal);
        long count = images.countByPetId(id);
        if (count >= 6) throw ApiException.badRequest("Cada mascota puede tener hasta 6 fotografías.");
        StoredFile stored = files.store(file);
        PetImage image = new PetImage(); image.setPet(pet); image.setStoragePath(stored.path());
        image.setOriginalFilename(stored.originalFilename()); image.setContentType(stored.contentType()); image.setFileSize(stored.size());
        image.setDisplayOrder((short) count); image.setPrimary(count == 0);
        return PetImageResponse.from(images.save(image));
    }

    @Transactional
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
        if (image.getPet().getAdoptionStatus() != AdoptionStatus.AVAILABLE && !owns(image.getPet(), principal)) throw ApiException.notFound("Imagen no encontrada.");
        return image;
    }

    @Transactional(readOnly=true)
    public List<PetImageResponse> images(UUID petId, SecurityUser principal) {
        Pet pet = find(petId);
        if (pet.getAdoptionStatus() != AdoptionStatus.AVAILABLE && !owns(pet, principal)) throw ApiException.notFound("Mascota no encontrada.");
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
        pet.setAdoptionStatus(status);
        if (status == AdoptionStatus.AVAILABLE && pet.getPublishedAt() == null) pet.setPublishedAt(Instant.now());
    }
    private void apply(Pet pet, PetRequest request, boolean creating) {
        pet.setName(request.name().trim()); pet.setSpecies(request.species()); pet.setBreed(request.breed());
        pet.setSex(request.sex()); pet.setAgeValue(request.ageValue()); pet.setAgeUnit(request.ageUnit()); pet.setSize(request.size());
        pet.setWeight(request.weight()); pet.setColor(request.color()); pet.setDescription(request.description().trim());
        pet.setPersonality(request.personality()); pet.setHealthSummary(request.healthSummary()); pet.setSterilized(request.sterilized());
        pet.setVaccinated(request.vaccinated()); pet.setDewormed(request.dewormed()); pet.setLocation(request.location());
    }
    private PetResponse response(Pet pet) {
        List<PetImageResponse> photoList = images.findAllByPetIdOrderByDisplayOrder(pet.getId()).stream().map(PetImageResponse::from).toList();
        return PetResponse.from(pet, photoList);
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
