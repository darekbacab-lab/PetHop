package com.pethop.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.Pet;
import com.pethop.pet.PetImageRepository;
import com.pethop.pet.PetRepository;
import com.pethop.pet.PetSex;
import com.pethop.pet.PetSize;
import com.pethop.pet.PetSpecies;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterProfile;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    @Mock FavoriteRepository favorites;
    @Mock PetRepository pets;
    @Mock PetImageRepository images;
    @InjectMocks FavoriteService service;

    @Test
    void listingFavoritesProjectsPetAndShelterFieldsWithoutReturningEntities() {
        User adopter = new User();
        ReflectionTestUtils.setField(adopter, "id", UUID.randomUUID());
        adopter.setRole(UserRole.ADOPTER);
        User shelterUser = new User();
        ReflectionTestUtils.setField(shelterUser, "id", UUID.randomUUID());
        ShelterProfile shelter = new ShelterProfile();
        ReflectionTestUtils.setField(shelter, "id", UUID.randomUUID());
        shelter.setUser(shelterUser);
        shelter.setOrganizationName("Refugio del Caribe");
        Pet pet = new Pet();
        ReflectionTestUtils.setField(pet, "id", UUID.randomUUID());
        pet.setShelter(shelter);
        pet.setName("Nube");
        pet.setSpecies(PetSpecies.CAT);
        pet.setSex(PetSex.FEMALE);
        pet.setSize(PetSize.SMALL);
        pet.setDescription("Busca un hogar.");
        pet.setAdoptionStatus(AdoptionStatus.AVAILABLE);
        pet.setPersonalityTraits(List.of("Tranquila"));
        Favorite favorite = new Favorite();
        favorite.setUser(adopter);
        favorite.setPet(pet);
        when(favorites.findAllByUserIdOrderByCreatedAtDesc(adopter.getId())).thenReturn(List.of(favorite));
        when(images.findAllByPetIdOrderByDisplayOrder(pet.getId())).thenReturn(List.of());

        var response = service.list(new SecurityUser(adopter));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().name()).isEqualTo("Nube");
        assertThat(response.getFirst().shelterName()).isEqualTo("Refugio del Caribe");
        assertThat(response.getFirst().personalityTraits()).containsExactly("Tranquila");
        assertThat(response.getFirst().images()).isEmpty();
    }
}
