package com.pethop.health;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.pethop.adoption.AdoptionRepository;
import com.pethop.common.exception.ApiException;
import com.pethop.health.dto.HealthRecordRequest;
import com.pethop.health.dto.HealthRecordResponse;
import com.pethop.pet.Pet;
import com.pethop.pet.PetRepository;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterProfile;
import com.pethop.shelter.ShelterRepository;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {
    @Mock HealthRecordRepository records;
    @Mock PetRepository pets;
    @Mock ShelterRepository shelters;
    @Mock AdoptionRepository adoptions;
    private HealthRecordService service;
    private Pet pet;
    private ShelterProfile shelter;
    private User shelterUser;
    private PetHealthRecord record;

    @BeforeEach void setUp() {
        service = new HealthRecordService(records, pets, shelters, adoptions);
        shelterUser = new User();
        ReflectionTestUtils.setField(shelterUser, "id", UUID.randomUUID());
        shelterUser.setRole(UserRole.SHELTER);
        shelter = new ShelterProfile();
        ReflectionTestUtils.setField(shelter, "id", UUID.randomUUID());
        shelter.setUser(shelterUser);
        pet = new Pet();
        ReflectionTestUtils.setField(pet, "id", UUID.randomUUID());
        pet.setShelter(shelter);
        record = new PetHealthRecord();
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
        record.setPet(pet);
        record.setRecordType(HealthRecordType.VACCINATION);
        record.setTitle("Vacuna antirrábica");
        record.setPerformedAt(LocalDate.of(2026, 1, 10));
        lenient().when(pets.findByIdAndDeletedAtIsNull(pet.getId())).thenReturn(Optional.of(pet));
        lenient().when(shelters.findByUserId(shelterUser.getId())).thenReturn(Optional.of(shelter));
        lenient().when(records.findById(record.getId())).thenReturn(Optional.of(record));
        lenient().when(records.save(any(PetHealthRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void updateChangesTitleAndDatesForOwnerShelter() {
        HealthRecordResponse updated = service.update(new SecurityUser(shelterUser), pet.getId(), record.getId(),
                new HealthRecordRequest(HealthRecordType.VACCINATION, "Vacuna antirrábica (refuerzo)", null,
                        LocalDate.of(2026, 1, 12), LocalDate.of(2027, 1, 12), "Dra. Ruiz"));

        Assertions.assertThat(updated.title()).isEqualTo("Vacuna antirrábica (refuerzo)");
        Assertions.assertThat(updated.performedAt()).isEqualTo(LocalDate.of(2026, 1, 12));
        Assertions.assertThat(updated.nextDueAt()).isEqualTo(LocalDate.of(2027, 1, 12));
        Assertions.assertThat(updated.veterinarianName()).isEqualTo("Dra. Ruiz");
        verify(records).save(any(PetHealthRecord.class));
    }

    @Test void updateRejectsNextDueBeforePerformed() {
        Assertions.assertThatThrownBy(() -> service.update(new SecurityUser(shelterUser), pet.getId(), record.getId(),
                        new HealthRecordRequest(HealthRecordType.VACCINATION, "Vacuna", null,
                                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1), null)))
                .isInstanceOf(ApiException.class).hasMessage("La próxima fecha no puede ser anterior a la fecha realizada.");
        verify(records, never()).save(any());
    }

    @Test void updateAndDeleteAreForbiddenForNonOwners() {
        User stranger = new User();
        ReflectionTestUtils.setField(stranger, "id", UUID.randomUUID());
        stranger.setRole(UserRole.ADOPTER);
        lenient().when(shelters.findByUserId(stranger.getId())).thenReturn(Optional.empty());
        lenient().when(adoptions.existsByPetIdAndAdopterId(pet.getId(), stranger.getId())).thenReturn(false);

        Assertions.assertThatThrownBy(() -> service.update(new SecurityUser(stranger), pet.getId(), record.getId(),
                        new HealthRecordRequest(HealthRecordType.VACCINATION, "Vacuna", null, LocalDate.of(2026, 1, 10), null, null)))
                .isInstanceOf(ApiException.class);
        Assertions.assertThatThrownBy(() -> service.delete(new SecurityUser(stranger), pet.getId(), record.getId()))
                .isInstanceOf(ApiException.class);
        verify(records, never()).save(any());
        verify(records, never()).delete(any());
    }

    @Test void deleteRemovesTheRecordForOwnerShelter() {
        service.delete(new SecurityUser(shelterUser), pet.getId(), record.getId());
        verify(records).delete(record);
    }
}
