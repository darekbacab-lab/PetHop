package com.pethop.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Regresión: la sesión debe vivir en PostgreSQL (Spring Session JDBC) y no solo
 * en la memoria del backend. Antes, cada reinicio o redespliegue de Railway
 * borraba todas las sesiones y al recargar la página el usuario aparecía
 * deslogueado.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class SessionPersistenceTest {
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

    @Test
    void loginCreatesDatabaseSessionAndItRestoresTheUser() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = userId + "@example.test";
        String passwordHash = new BCryptPasswordEncoder(12).encode("Secreto123");
        jdbc.update("insert into users(id, full_name, email, password_hash, role) values (?, ?, ?, ?, 'ADOPTER')",
                userId, "Adoptante de prueba", email, passwordHash);

        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Secreto123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie sessionCookie = login.getResponse().getCookie("PETHOPSESSION");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.getValue()).isNotBlank();

        // La cookie PETHOPSESSION guarda el id de sesión codificado en Base64
        // (DefaultCookieSerializer, useBase64Encoding=true por defecto); en la
        // tabla spring_session el session_id es el UUID sin codificar.
        String sessionId = new String(Base64.getDecoder().decode(sessionCookie.getValue()));
        Integer sessions = jdbc.queryForObject(
                "select count(*) from spring_session where session_id = ?", Integer.class, sessionId);
        assertThat(sessions).isEqualTo(1);

        mvc.perform(get("/api/v1/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk());
    }
}
