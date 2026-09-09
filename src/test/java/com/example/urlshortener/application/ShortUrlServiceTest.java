package com.example.urlshortener.application;

import com.example.urlshortener.application.exception.*;
import com.example.urlshortener.domain.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.net.URI;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShortUrlServiceTest {
    @Mock
    ShortUrlRepository repository;
    @Mock
    RedirectCache cache;
    @Mock
    ShortCodeGenerator generator;
    @Mock
    ShortUrlEventPublisher events;
    private AutoCloseable mocks;
    private ShortUrlService service;
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new ShortUrlService(repository, cache, generator, Clock.fixed(now, ZoneOffset.UTC), events);
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    @Test
    void createsWithCustomAlias() {
        var command = new CreateShortUrlCommand(URI.create("https://example.com"), "my-link", null);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.create(command).shortCode()).isEqualTo("my-link");
    }

    @Test
    void rejectsDuplicateAlias() {
        when(repository.existsByShortCode("taken")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateShortUrlCommand(URI.create("https://example.com"), "taken", null))).isInstanceOf(DuplicateShortCodeException.class);
    }

    @Test
    void retriesGeneratedCollision() {
        when(generator.generate()).thenReturn("AAAA1111", "BBBB2222");
        when(repository.existsByShortCode("AAAA1111")).thenReturn(true);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.create(new CreateShortUrlCommand(URI.create("https://example.com"), null, null)).shortCode()).isEqualTo("BBBB2222");
    }

    @Test
    void resolvesFromDatabaseAndCaches() {
        var link = link("code1234", null);
        when(cache.get("code1234")).thenReturn(Optional.empty());
        when(repository.findByShortCode("code1234")).thenReturn(Optional.of(link));
        assertThat(service.resolve("code1234")).isEqualTo(link.originalUrl());
        verify(repository).incrementAccessCount("code1234");
        verify(events).visited(link, now);
        verify(cache).put(eq("code1234"), eq(link.originalUrl()), eq(Duration.ofHours(24)));
    }

    @Test
    void rejectsExpiredUrl() {
        when(cache.get("expired1")).thenReturn(Optional.empty());
        when(repository.findByShortCode("expired1")).thenReturn(Optional.of(link("expired1", now)));
        assertThatThrownBy(() -> service.resolve("expired1")).isInstanceOf(ShortUrlExpiredException.class);
        verify(repository, never()).incrementAccessCount(any());
        verifyNoInteractions(events);
    }

    @Test
    void rejectsMissingCode() {
        when(cache.get("missing1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve("missing1")).isInstanceOf(ShortUrlNotFoundException.class);
    }

    @Test
    void publishesPersistedIdentityOnCreation() {
        ShortUrl saved = link("created1", null);
        when(repository.save(any())).thenReturn(saved);
        service.create(new CreateShortUrlCommand(saved.originalUrl(), "created1", null));
        verify(events).created(saved);
    }

    @Test
    void publishesVisitOnCacheHit() {
        ShortUrl saved = link("cached12", null);
        when(repository.findByShortCode("cached12")).thenReturn(Optional.of(saved));
        when(cache.get("cached12")).thenReturn(Optional.of(saved.originalUrl()));
        assertThat(service.resolve("cached12")).isEqualTo(saved.originalUrl());
        verify(events).visited(saved, now);
        verify(cache, never()).put(any(), any(), any());
    }

    private ShortUrl link(String code, Instant expires) {
        return new ShortUrl(1L, code, URI.create("https://example.com/path"), now.minusSeconds(60), expires, 0);
    }
}
