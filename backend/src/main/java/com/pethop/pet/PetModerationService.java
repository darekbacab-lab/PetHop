package com.pethop.pet;

import com.pethop.common.exception.ApiException;
import com.pethop.common.response.PageResponse;
import com.pethop.admin.AdminAuditService;
import com.pethop.notification.NotificationService;
import com.pethop.pet.dto.ModerationDecisionRequest;
import com.pethop.pet.dto.ModerationHistoryResponse;
import com.pethop.pet.dto.PetImageResponse;
import com.pethop.pet.dto.PetResponse;
import com.pethop.security.SecurityUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetModerationService {
    private final PetRepository pets;
    private final PetImageRepository images;
    private final PetModerationHistoryRepository history;
    private final NotificationService notifications;
    private final AdminAuditService audit;
    public PetModerationService(PetRepository pets, PetImageRepository images, PetModerationHistoryRepository history,
            NotificationService notifications, AdminAuditService audit) {
        this.pets = pets; this.images = images; this.history = history; this.notifications = notifications; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<PetResponse> pending(int page, int size) {
        Specification<Pet> spec = PetSpecifications.filters(null, null, null, null, null, null, null,
                ModerationStatus.PENDING);
        return PageResponse.from(pets.findAll(spec, PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "createdAt"))), this::response);
    }

    @Transactional(readOnly = true)
    public PetResponse get(UUID id) { return response(find(id)); }

    @Transactional
    public PetResponse decide(UUID id, SecurityUser admin, ModerationDecisionRequest request) {
        Pet pet = pets.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Mascota no encontrada."));
        if (pet.getModerationStatus() != ModerationStatus.PENDING && pet.getModerationStatus() != ModerationStatus.CHANGES_REQUESTED)
            throw ApiException.conflict("La publicación ya no está pendiente de moderación.");
        if ((request.action() == ModerationAction.REJECT || request.action() == ModerationAction.REQUEST_CORRECTIONS)
                && (request.reason() == null || request.reason().isBlank()))
            throw ApiException.badRequest("Debe indicar el motivo de la decisión.");
        ModerationStatus previous = pet.getModerationStatus();
        ModerationStatus next = switch (request.action()) {
            case APPROVE -> ModerationStatus.APPROVED;
            case REJECT -> ModerationStatus.REJECTED;
            case REQUEST_CORRECTIONS -> ModerationStatus.CHANGES_REQUESTED;
        };
        pet.setModerationStatus(next); pet.setModerationReason(trim(request.reason())); pet.setModeratedAt(Instant.now());
        pets.save(pet);
        PetModerationHistory item = new PetModerationHistory(); item.setPet(pet); item.setAdmin(admin.user());
        item.setAction(request.action()); item.setPreviousStatus(previous); item.setNewStatus(next); item.setReason(trim(request.reason()));
        history.save(item);
        audit.record(admin.user(), "PET_MODERATION_" + request.action().name(), "PET", pet.getId(), previous.name(), next.name(), trim(request.reason()));
        String title = switch (request.action()) { case APPROVE -> "Publicación aprobada"; case REJECT -> "Publicación rechazada"; default -> "Correcciones solicitadas"; };
        String message = request.reason() == null || request.reason().isBlank() ? title + "." : title + ": " + request.reason().trim();
        notifications.create(pet.getShelter().getUser(), "PET_MODERATION", title, message, "PET", pet.getId());
        return response(pet);
    }

    @Transactional(readOnly = true)
    public List<ModerationHistoryResponse> history(UUID id) {
        find(id);
        return history.findAllByPetIdOrderByCreatedAtDesc(id).stream().map(ModerationHistoryResponse::from).toList();
    }

    private Pet find(UUID id) { return pets.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> ApiException.notFound("Mascota no encontrada.")); }
    private PetResponse response(Pet pet) {
        List<PetImageResponse> photos = images.findAllByPetIdOrderByDisplayOrder(pet.getId()).stream().map(PetImageResponse::from).toList();
        return PetResponse.from(pet, photos, pet.getPersonalityTraits());
    }
    private String trim(String value) { return value == null ? null : value.trim(); }
}
