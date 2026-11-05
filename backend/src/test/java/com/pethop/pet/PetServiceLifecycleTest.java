package com.pethop.pet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.pethop.adoption.AdoptionRepository;
import com.pethop.adoption.ApplicationRepository;
import com.pethop.adoption.ApplicationStatus;
import com.pethop.common.exception.ApiException;
import com.pethop.pet.storage.FileStorage;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterProfile;
import com.pethop.shelter.ShelterRepository;
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
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class PetServiceLifecycleTest {
    @Mock PetRepository pets;
    @Mock PetImageRepository images;
    @Mock ShelterRepository shelters;
    @Mock FileStorage files;
    @Mock ApplicationRepository applications;
    @Mock AdoptionRepository adoptions;

    private PetService service;
    private Pet pet;
    private SecurityUser admin;

    @BeforeEach
    void setUp() {
        service = new PetService(pets, images, shelters, files, applications, adoptions);
        pet = new Pet();
        ReflectionTestUtils.setField(pet, "id", UUID.randomUUID());
        pet.setAdoptionStatus(AdoptionStatus.AVAILABLE);
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setRole(UserRole.ADMIN);
        admin = new SecurityUser(user);
        lenient().when(pets.findByIdForUpdate(pet.getId())).thenReturn(Optional.of(pet));
    }

    @Test
    void adoptedPetCannotBePublishedOrEdited() {
        pet.setAdoptionStatus(AdoptionStatus.ADOPTED);

        assertThatThrownBy(() -> service.updateStatus(pet.getId(), admin, AdoptionStatus.AVAILABLE))
                .isInstanceOf(ApiException.class)
                .hasMessage("La mascota ya está comprometida con una adopción.");
        assertThatThrownBy(() -> service.update(pet.getId(), admin, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("La mascota ya está comprometida con una adopción.");

        verify(pets, never()).save(any(Pet.class));
    }

    @Test
    void petWithApprovedApplicationCannotBeMadeAvailable() {
        pet.setAdoptionStatus(AdoptionStatus.UNAVAILABLE);
        when(applications.existsByPetIdAndStatusIn(pet.getId(),
                java.util.List.of(ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED,
                        ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED))).thenReturn(true);

        assertThatThrownBy(() -> service.updateStatus(pet.getId(), admin, AdoptionStatus.AVAILABLE))
                .isInstanceOf(ApiException.class)
                .hasMessage("La mascota tiene una solicitud de adopción activa.");

        verify(pets, never()).save(any(Pet.class));
    }

    @Test
    void petInProcessCannotBeDeleted() {
        pet.setAdoptionStatus(AdoptionStatus.IN_PROCESS);

        assertThatThrownBy(() -> service.delete(pet.getId(), admin))
                .isInstanceOf(ApiException.class)
                .hasMessage("La mascota ya está comprometida con una adopción.");

        verify(pets, never()).save(any(Pet.class));
        verify(applications, never()).existsByPetIdAndStatusIn(any(), anyList());
    }

    @Test
    void petWithActiveApplicationCannotBeDeleted() {
        when(applications.existsByPetIdAndStatusIn(pet.getId(),
                java.util.List.of(ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED,
                        ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED))).thenReturn(true);

        assertThatThrownBy(() -> service.delete(pet.getId(), admin))
                .isInstanceOf(ApiException.class)
                .hasMessage("La mascota tiene solicitudes de adopción activas.");

        verify(pets, never()).save(any(Pet.class));
        verify(adoptions, never()).existsByPetId(any());
    }

    @Test
    void petWithRecordedAdoptionCannotBeDeleted() {
        when(adoptions.existsByPetId(pet.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(pet.getId(), admin))
                .isInstanceOf(ApiException.class)
                .hasMessage("La mascota ya tiene una adopción registrada.");

        verify(pets, never()).save(any(Pet.class));
    }

    @Test
    void shelterPetListingRequiresItsProfile() {
        User shelterUser = new User();
        UUID shelterUserId = UUID.randomUUID();
        ReflectionTestUtils.setField(shelterUser, "id", shelterUserId);
        shelterUser.setRole(UserRole.SHELTER);
        when(shelters.findByUserId(shelterUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listMine(null, null, null, null, null, null, 0, 24,
                new SecurityUser(shelterUser)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Perfil de refugio no encontrado.");
    }

    @Test
    void shelterPetListingDoesNotFilterOutPrivateLifecycleStates() {
        User shelterUser = new User();
        UUID shelterUserId = UUID.randomUUID();
        ReflectionTestUtils.setField(shelterUser, "id", shelterUserId);
        shelterUser.setRole(UserRole.SHELTER);
        ShelterProfile shelter = new ShelterProfile();
        ReflectionTestUtils.setField(shelter, "id", UUID.randomUUID());
        shelter.setUser(shelterUser);
        when(shelters.findByUserId(shelterUserId)).thenReturn(Optional.of(shelter));
        when(pets.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        service.listMine(null, null, null, null, null, null, 0, 24, new SecurityUser(shelterUser));

        verify(pets).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class));
    }
}
