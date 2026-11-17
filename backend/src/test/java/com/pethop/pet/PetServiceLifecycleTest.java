package com.pethop.pet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
    void publicListShowsOnlyApprovedAvailablePetsEvenForAdmins() {
        ArgumentCaptor<Specification<Pet>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        when(pets.findAll(specCaptor.capture(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.list(null, null, null, null, null, "newest", 0, 48);

        Specification<Pet> spec = specCaptor.getValue();
        Root<Pet> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> moderationPath = mock(Path.class);
        Path<Object> statusPath = mock(Path.class);
        Path<Object> deletedPath = mock(Path.class);
        when(root.get("moderationStatus")).thenReturn(moderationPath);
        when(root.get("adoptionStatus")).thenReturn(statusPath);
        when(root.get("deletedAt")).thenReturn(deletedPath);
        when(cb.equal(moderationPath, ModerationStatus.APPROVED)).thenReturn(mock(Predicate.class));
        when(cb.equal(statusPath, AdoptionStatus.AVAILABLE)).thenReturn(mock(Predicate.class));
        when(cb.isNull(deletedPath)).thenReturn(mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        spec.toPredicate(root, query, cb);

        verify(cb).equal(moderationPath, ModerationStatus.APPROVED);
        verify(cb).equal(statusPath, AdoptionStatus.AVAILABLE);
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
