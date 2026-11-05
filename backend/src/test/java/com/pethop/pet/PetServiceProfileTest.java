package com.pethop.pet;

import static org.assertj.core.api.Assertions.assertThat;

import com.pethop.pet.dto.PetRequest;
import com.pethop.pet.dto.PetResponse;
import com.pethop.shelter.ShelterProfile;
import com.pethop.user.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PetServiceProfileTest {
    @Test
    void petRequestFieldsSurviveServiceMappingAndResponseProjection() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ShelterProfile shelter = new ShelterProfile();
        ReflectionTestUtils.setField(shelter, "id", UUID.randomUUID());
        shelter.setUser(user);
        shelter.setOrganizationName("Refugio");
        Pet pet = new Pet();
        pet.setShelter(shelter);

        PetRequest request = new PetRequest("Nube", PetSpecies.CAT, "Mestiza", PetSex.FEMALE,
                null, null, PetSize.SMALL, null, "Blanco", "Descripción independiente", "Tranquila, Convive con gatos",
                "Observación clínica", null, true, false, "Cozumel", AdoptionStatus.AVAILABLE,
                PetLifeStage.SENIOR, List.of("Tranquila", "Convive con gatos"), "Condición conocida",
                "Llegó por rescate", "Antecedente separado", "Casa", "Patio seguro", "Con experiencia", "Ventanas protegidas");

        PetService service = new PetService(null, null, null, null, null, null);
        ReflectionTestUtils.invokeMethod(service, "apply", pet, request, true);
        pet.setAdoptionStatus(request.adoptionStatus());
        PetResponse response = PetResponse.from(pet, List.of(), List.copyOf(pet.getPersonalityTraits()));

        assertThat(response.lifeStage()).isEqualTo(PetLifeStage.SENIOR);
        assertThat(response.ageValue()).isNull();
        assertThat(response.personalityTraits()).containsExactly("Tranquila", "Convive con gatos");
        assertThat(response.location()).isEqualTo("Cozumel");
        assertThat(response.description()).isEqualTo("Descripción independiente");
        assertThat(response.story()).isEqualTo("Llegó por rescate");
        assertThat(response.background()).isEqualTo("Antecedente separado");
        assertThat(response.homeType()).isEqualTo("Casa");
        assertThat(response.conditions()).isEqualTo("Patio seguro");
        assertThat(response.experience()).isEqualTo("Con experiencia");
        assertThat(response.requirements()).isEqualTo("Ventanas protegidas");
        assertThat(response.vaccinated()).isTrue();
        assertThat(response.sterilized()).isNull();
        assertThat(response.dewormed()).isFalse();
    }
}
