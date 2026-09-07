package com.example.urlshortener.domain;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.*;

import static org.assertj.core.api.Assertions.*;

class ShortUrlTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void detectsExpirationAtBoundary() {
        var link = new ShortUrl(1L, "abcd", URI.create("https://example.com"), NOW.minusSeconds(10), NOW, 0);
        assertThat(link.isExpired(clock)).isTrue();
    }

    @Test
    void acceptsNonExpiredLink() {
        var link = new ShortUrl(1L, "abcd", URI.create("https://example.com"), NOW, NOW.plusSeconds(1), 0);
        assertThat(link.isExpired(clock)).isFalse();
    }

    @Test
    void rejectsNonHttpUrl() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ShortUrl(null, "abcd", URI.create("ftp://example.com/a"), NOW, null, 0));
    }
}
