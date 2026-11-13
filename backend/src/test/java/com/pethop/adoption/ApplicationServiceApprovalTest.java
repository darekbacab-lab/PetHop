package com.pethop.adoption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pethop.common.exception.ApiException;
import com.pethop.notification.NotificationService;
import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.Pet;
import com.pethop.pet.PetRepository;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterProfile;
import com.pethop.shelter.ShelterRepository;
import com.pethop.shelter.VerificationStatus;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceApprovalTest {
    @Mock ApplicationRepository applications;
    @Mock ApplicationAnswerRepository answers;
    @Mock ApplicationStatusHistoryRepository history;
    @Mock PetRepository pets;
    @Mock ShelterRepository shelters;
    @Mock NotificationService notifications;

    private ApplicationService service;
    private AdoptionApplication application;
    private Pet pet;
    private SecurityUser shelterPrincipal;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applications, answers, history, pets, shelters, notifications);

        User shelterUser = user(UserRole.SHELTER);
        shelterPrincipal = new SecurityUser(shelterUser);
        ShelterProfile shelter = new ShelterProfile();
        ReflectionTestUtils.setField(shelter, "id", UUID.randomUUID());
        shelter.setUser(shelterUser);
        shelter.setVerificationStatus(VerificationStatus.VERIFIED);
        when(shelters.findByUserId(shelterUser.getId())).thenReturn(Optional.of(shelter));

        pet = new Pet();
        ReflectionTestUtils.setField(pet, "id", UUID.randomUUID());
        pet.setShelter(shelter);
        pet.setName("Nube");
        pet.setAdoptionStatus(AdoptionStatus.AVAILABLE);

        application = new AdoptionApplication();
        ReflectionTestUtils.setField(application, "id", UUID.randomUUID());
        application.setPet(pet);
        application.setShelter(shelter);
        application.setAdopter(user(UserRole.ADOPTER));
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setMotivation("Quiere adoptar");
    }

    @Test
    void approvesAvailablePetUsingThePessimisticPetLock() {
        when(applications.findById(application.getId())).thenReturn(Optional.of(application));
        when(pets.findByIdForUpdate(pet.getId())).thenReturn(Optional.of(pet));

        service.updateStatus(shelterPrincipal, application.getId(), ApplicationStatus.APPROVED, "Aprobada");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(pet.getAdoptionStatus()).isEqualTo(AdoptionStatus.IN_PROCESS);
        verify(pets).findByIdForUpdate(pet.getId());
        verify(history).save(org.mockito.ArgumentMatchers.any(ApplicationStatusHistory.class));
        verify(notifications).create(org.mockito.ArgumentMatchers.eq(application.getAdopter()),
                org.mockito.ArgumentMatchers.eq("APPLICATION_STATUS_CHANGED"), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("APPLICATION"),
                org.mockito.ArgumentMatchers.eq(application.getId()));
    }

    @ParameterizedTest
    @EnumSource(value = AdoptionStatus.class, names = {"IN_PROCESS", "ADOPTED", "UNAVAILABLE"})
    void rejectsApprovalWhenPetIsNotAvailable(AdoptionStatus status) {
        pet.setAdoptionStatus(status);
        when(applications.findById(application.getId())).thenReturn(Optional.of(application));
        when(pets.findByIdForUpdate(pet.getId())).thenReturn(Optional.of(pet));

        assertThatThrownBy(() -> service.updateStatus(shelterPrincipal, application.getId(), ApplicationStatus.APPROVED, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("La mascota ya no está disponible para aprobación.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        verify(applications, never()).save(org.mockito.ArgumentMatchers.any());
        verify(history, never()).save(org.mockito.ArgumentMatchers.any());
        verify(notifications, never()).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unverifiedShelterCannotApproveApplications() {
        application.getShelter().setVerificationStatus(VerificationStatus.PENDING);
        when(applications.findById(application.getId())).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.updateStatus(shelterPrincipal, application.getId(), ApplicationStatus.APPROVED, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Tu refugio debe estar verificado para aprobar solicitudes.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        verify(applications, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void secondApplicationForSamePetCannotBeApprovedAfterFirstApproval() {
        when(applications.findById(application.getId())).thenReturn(Optional.of(application));
        when(pets.findByIdForUpdate(pet.getId())).thenReturn(Optional.of(pet));
        service.updateStatus(shelterPrincipal, application.getId(), ApplicationStatus.APPROVED, null);

        AdoptionApplication second = new AdoptionApplication();
        ReflectionTestUtils.setField(second, "id", UUID.randomUUID());
        second.setPet(pet);
        second.setShelter(application.getShelter());
        second.setAdopter(user(UserRole.ADOPTER));
        second.setStatus(ApplicationStatus.SUBMITTED);
        second.setMotivation("Otra solicitud");
        when(applications.findById(second.getId())).thenReturn(Optional.of(second));

        assertThatThrownBy(() -> service.updateStatus(shelterPrincipal, second.getId(), ApplicationStatus.APPROVED, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("La mascota ya no está disponible para aprobación.");
        assertThat(second.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void preservesAuthorizationAndExistingStatusTransitions() {
        when(applications.findById(application.getId())).thenReturn(Optional.of(application));
        assertThatThrownBy(() -> service.updateStatus(new SecurityUser(user(UserRole.ADOPTER)), application.getId(),
                ApplicationStatus.REJECTED, null))
                .isInstanceOf(ApiException.class);

        service.updateStatus(shelterPrincipal, application.getId(), ApplicationStatus.UNDER_REVIEW, "Revisión iniciada");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        verify(pets, never()).findByIdForUpdate(org.mockito.ArgumentMatchers.any());
    }

    private static User user(UserRole role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setRole(role);
        user.setFullName(role.name());
        user.setEmail(role.name().toLowerCase() + "@example.test");
        return user;
    }
}
