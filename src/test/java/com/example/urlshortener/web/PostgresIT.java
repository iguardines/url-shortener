package com.example.urlshortener.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.Flyway;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles(value = "prod", inheritProfiles = false)
class PostgresIT extends UrlShortenerIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("DB_URL", postgres::getJdbcUrl);
        properties.add("DB_USERNAME", postgres::getUsername);
        properties.add("DB_PASSWORD", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @Test
    void migrationsAreRepeatableWithoutDeletingExistingData() {
        jdbc.update("INSERT INTO short_urls(short_code, original_url, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                "migration-check", "https://example.com");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT original_url FROM short_urls WHERE short_code = ?",
                String.class, "migration-check")).isEqualTo("https://example.com");
    }
}
