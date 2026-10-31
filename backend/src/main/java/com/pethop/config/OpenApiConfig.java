package com.pethop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI petHopOpenApi() {
        return new OpenAPI().info(new Info().title("PetHop API").version("v1")
                .description("API REST de adopción responsable. La autenticación usa sesión segura y protección CSRF."));
    }
}
