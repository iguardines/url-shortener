package com.example.urlshortener.web;

import com.example.urlshortener.application.*;
import com.example.urlshortener.domain.ShortCodeGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:collisiontest;DB_CLOSE_DELAY=-1")
class CollisionIntegrationTest {
    @Autowired
    ShortUrlService service;
    @MockitoBean
    ShortCodeGenerator generator;
    @MockitoSpyBean
    ShortUrlRepository repository;

    @Test
    void retriesAnActualUniqueConstraintViolationInANewTransaction() {
        var url = URI.create("https://example.com");
        service.create(new CreateShortUrlCommand(url, "collision", null));
        // Simulate a concurrent insert between the existence check and insert.
        doReturn(false).when(repository).existsByShortCode("collision");
        when(generator.generate()).thenReturn("collision", "nextCode");
        var result = service.create(new CreateShortUrlCommand(url, null, null));
        assertThat(result.shortCode()).isEqualTo("nextCode");
        assertThat(service.get("nextCode").originalUrl()).isEqualTo(url);
        assertThat(service.get("collision").originalUrl()).isEqualTo(url);
    }
}
