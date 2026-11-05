package com.pethop.veterinary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pethop.adoption.AdoptionRepository;
import com.pethop.common.exception.ApiException;
import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.ModerationStatus;
import com.pethop.pet.Pet;
import com.pethop.pet.PetRepository;
import com.pethop.pet.PetSpecies;
import com.pethop.security.SecurityUser;
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

@ExtendWith(MockitoExtension.class)
class VeterinaryAssistantServiceTest {
    @Mock PetRepository pets;
    @Mock ShelterRepository shelters;
    @Mock AdoptionRepository adoptions;
    @Mock VeterinaryAiProvider provider;
    private VeterinaryAssistantService service;
    private Pet pet;
    private SecurityUser user;

    @BeforeEach
    void setUp() {
        service = new VeterinaryAssistantService(pets, shelters, adoptions, provider, new ObjectMapper());
        pet = new Pet();
        ReflectionTestUtils.setField(pet, "id", UUID.randomUUID());
        pet.setName("Luna"); pet.setSpecies(PetSpecies.DOG); pet.setAdoptionStatus(AdoptionStatus.AVAILABLE);
        pet.setModerationStatus(ModerationStatus.APPROVED); pet.setDescription("Una perrita tranquila.");
        User account = new User(); ReflectionTestUtils.setField(account, "id", UUID.randomUUID()); account.setRole(UserRole.ADOPTER);
        user = new SecurityUser(account);
        lenient().when(pets.findByIdAndDeletedAtIsNull(pet.getId())).thenReturn(Optional.of(pet));
    }

    @Test
    void parsesProviderResponseAndKeepsFixedDisclaimer() {
        when(provider.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
                {"urgency":"YELLOW","summary":"Conviene consultar al veterinario.","reasons":["Hay un cambio persistente."],"warning_signs":["Empeoramiento rápido."],"guidance":["Observa y agenda una evaluación."]}
                """);

        var response = service.ask(user, pet.getId(), "Come menos desde ayer");

        assertThat(response.urgency()).isEqualTo(VeterinaryUrgency.YELLOW);
        assertThat(response.petName()).isEqualTo("Luna");
        assertThat(response.disclaimer()).contains("no sustituye");
    }

    @Test
    void publicAvailablePetCanBeConsultedWithoutAuthenticatedUser() {
        when(provider.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
                {"urgency":"GREEN","summary":"No se observan señales de alarma en la descripción.","reasons":[],"warning_signs":[],"guidance":["Observa cualquier cambio y consulta si persiste."]}
                """);

        var response = service.ask(null, pet.getId(), "¿Qué cuidados preventivos necesita?");

        assertThat(response.petId()).isEqualTo(pet.getId());
        verify(provider).generate(org.mockito.ArgumentMatchers.contains("especie=DOG"));
    }

    @Test
    void generalQuestionCanBeAnsweredWithoutPetContext() {
        when(provider.generate(org.mockito.ArgumentMatchers.contains("No hay una mascota específica seleccionada"))).thenReturn("""
                {"urgency":"GREEN","summary":"Puedes mantener una rutina preventiva.","reasons":[],"warning_signs":[],"guidance":["Consulta a un veterinario para un plan individual."]}
                """);

        var response = service.askGeneral(null, null, "¿Cómo cuido la higiene de una mascota?");

        assertThat(response.petId()).isNull();
        assertThat(response.urgency()).isEqualTo(VeterinaryUrgency.GREEN);
    }

    @Test
    void emergencyMessageGetsUrgentGuidanceWithoutTrustingProvider() {
        var response = service.ask(user, pet.getId(), "Tiene dificultad para respirar");

        assertThat(response.urgency()).isEqualTo(VeterinaryUrgency.RED);
        assertThat(response.guidance()).anyMatch(value -> value.contains("urgencias"));
        verify(provider, never()).generate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void malformedProviderResponseIsRejected() {
        when(provider.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("no es json");

        assertThatThrownBy(() -> service.ask(user, pet.getId(), "Está más cansado"))
                .isInstanceOf(ApiException.class).hasMessage("El servicio veterinario devolvió una respuesta no válida.");
    }

    @Test
    void acceptsJsonSurroundedByProviderTextAndCamelCaseWarningSigns() {
        when(provider.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
                Aquí tienes orientación preliminar:
                ```json
                {"urgency":"GREEN","summary":"Mantén observación preventiva.","reasons":[],"warningSigns":["Empeoramiento rápido."],"guidance":["Consulta si persiste."]}
                ```
                """);

        var response = service.ask(user, pet.getId(), "¿Qué cuidados necesita?");

        assertThat(response.warningSigns()).containsExactly("Empeoramiento rápido.");
    }

    @Test
    void providerFailureIsReturnedAsUnavailable() {
        when(provider.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(ApiException.serviceUnavailable("El servicio veterinario no está disponible en este momento."));

        assertThatThrownBy(() -> service.ask(user, pet.getId(), "Tiene comezón"))
                .isInstanceOf(ApiException.class).hasMessage("El servicio veterinario no está disponible en este momento.");
    }

    @Test
    void privatePetContextIsNotAvailableToUnrelatedUser() {
        pet.setAdoptionStatus(AdoptionStatus.IN_PROCESS);
        when(adoptions.existsByPetIdAndAdopterId(pet.getId(), user.id())).thenReturn(false);

        assertThatThrownBy(() -> service.context(user, pet.getId()))
                .isInstanceOf(ApiException.class).hasMessage("Mascota no encontrada.");
    }
}
