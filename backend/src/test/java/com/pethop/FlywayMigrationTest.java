package com.pethop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class FlywayMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayCreatesCoreTablesAndAllMigrations() {
        Integer tables = jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name in ('users','pets','adoption_applications','adoptions','follow_up_reports','favorites','messages','notifications')", Integer.class);
        Integer migrations = jdbc.queryForObject("select count(*) from flyway_schema_history where success = true", Integer.class);
        assertThat(tables).isEqualTo(8);
        assertThat(migrations).isEqualTo(14);
        Integer moderationHistoryTable = jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name='pet_moderation_history'", Integer.class);
        assertThat(moderationHistoryTable).isEqualTo(1);
        Integer profileColumns = jdbc.queryForObject("select count(*) from information_schema.columns where table_schema='public' and table_name='pets' and column_name in ('life_stage','adoption_story','background','home_type','housing_conditions','recommended_experience','adoption_requirements','health_condition')", Integer.class);
        Integer traitTable = jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name='pet_personality_traits'", Integer.class);
        Integer traitPositionType = jdbc.queryForObject("select count(*) from information_schema.columns where table_schema='public' and table_name='pet_personality_traits' and column_name='position' and data_type='integer'", Integer.class);
        assertThat(profileColumns).isEqualTo(8);
        assertThat(traitTable).isEqualTo(1);
        assertThat(traitPositionType).isEqualTo(1);
    }
}
