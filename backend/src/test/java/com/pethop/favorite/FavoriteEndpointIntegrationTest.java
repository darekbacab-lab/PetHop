package com.pethop.favorite;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pethop.security.SecurityUser;
import com.pethop.user.User;
import com.pethop.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteEndpointIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;

    @Test
    void authenticatedAdopterCanReadFavoritePetAndItsLazyProfileData() throws Exception {
        UUID adopterId = UUID.randomUUID();
        UUID shelterUserId = UUID.randomUUID();
        UUID shelterId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        jdbc.update("insert into users(id, full_name, email, password_hash, role) values (?, ?, ?, ?, 'ADOPTER')",
                adopterId, "Adoptante de prueba", adopterId + "@example.test", "test-hash");
        jdbc.update("insert into users(id, full_name, email, password_hash, role) values (?, ?, ?, ?, 'SHELTER')",
                shelterUserId, "Responsable", shelterUserId + "@example.test", "test-hash");
        jdbc.update("insert into shelter_profiles(id, user_id, organization_name) values (?, ?, ?)",
                shelterId, shelterUserId, "Refugio del Caribe");
        jdbc.update("insert into pets(id, shelter_id, name, species, sex, description, adoption_status) values (?, ?, ?, 'CAT', 'FEMALE', ?, 'AVAILABLE')",
                petId, shelterId, "Nube", "Busca un hogar.");
        jdbc.update("insert into pet_personality_traits(pet_id, position, trait) values (?, 0, ?)", petId, "Tranquila");
        jdbc.update("insert into favorites(id, user_id, pet_id) values (?, ?, ?)", UUID.randomUUID(), adopterId, petId);

        User adopter = users.findById(adopterId).orElseThrow();
        var principal = new SecurityUser(adopter);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities().stream().map(authority -> new SimpleGrantedAuthority(authority.getAuthority())).toList());

        mvc.perform(get("/api/v1/favorites").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nube"))
                .andExpect(jsonPath("$[0].shelterName").value("Refugio del Caribe"))
                .andExpect(jsonPath("$[0].personalityTraits[0]").value("Tranquila"));
    }
}
