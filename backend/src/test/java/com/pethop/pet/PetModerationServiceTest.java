package com.pethop.pet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.pethop.common.exception.ApiException;
import com.pethop.notification.NotificationService;
import com.pethop.admin.AdminAuditService;
import com.pethop.pet.dto.ModerationDecisionRequest;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterProfile;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PetModerationServiceTest {
    @Mock PetRepository pets;
    @Mock PetImageRepository images;
    @Mock PetModerationHistoryRepository history;
    @Mock NotificationService notifications;
    @Mock AdminAuditService audit;
    private PetModerationService service;
    private Pet pet;
    private SecurityUser admin;

    @BeforeEach void setUp() {
        service = new PetModerationService(pets, images, history, notifications, audit);
        User owner = new User(); ReflectionTestUtils.setField(owner, "id", UUID.randomUUID()); owner.setRole(UserRole.SHELTER);
        ShelterProfile shelter = new ShelterProfile(); ReflectionTestUtils.setField(shelter, "id", UUID.randomUUID()); shelter.setUser(owner);
        pet = new Pet(); ReflectionTestUtils.setField(pet, "id", UUID.randomUUID()); pet.setShelter(shelter);
        pet.setModerationStatus(ModerationStatus.PENDING);
        User moderator = new User(); ReflectionTestUtils.setField(moderator, "id", UUID.randomUUID()); moderator.setRole(UserRole.ADMIN); moderator.setFullName("Moderador");
        admin = new SecurityUser(moderator);
        lenient().when(pets.findByIdForUpdate(pet.getId())).thenReturn(Optional.of(pet));
        lenient().when(images.findAllByPetIdOrderByDisplayOrder(pet.getId())).thenReturn(java.util.List.of());
    }

    @Test void approveLocksPetPersistsHistoryAndNotifiesOwner() {
        service.decide(pet.getId(), admin, new ModerationDecisionRequest(ModerationAction.APPROVE, null));

        verify(pets).findByIdForUpdate(pet.getId());
        verify(history).save(any(PetModerationHistory.class));
        verify(notifications).create(any(User.class), eq("PET_MODERATION"), eq("Publicación aprobada"), anyString(), eq("PET"), eq(pet.getId()));
        org.assertj.core.api.Assertions.assertThat(pet.getModerationStatus()).isEqualTo(ModerationStatus.APPROVED);
    }

    @Test void rejectionRequiresReasonAndDoesNotWriteHistory() {
        assertThatThrownBy(() -> service.decide(pet.getId(), admin, new ModerationDecisionRequest(ModerationAction.REJECT, "  ")))
                .isInstanceOf(ApiException.class).hasMessage("Debe indicar el motivo de la decisión.");
        verify(history, never()).save(any());
        verify(notifications, never()).create(any(), anyString(), anyString(), anyString(), anyString(), any());
    }
}
