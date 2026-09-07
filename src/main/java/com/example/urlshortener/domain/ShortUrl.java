package com.example.urlshortener.domain;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public record ShortUrl(Long id, String shortCode, URI originalUrl, Instant createdAt, Instant expiresAt,
                       long accessCount) {
    public ShortUrl {
        Objects.requireNonNull(shortCode);
        Objects.requireNonNull(originalUrl);
        Objects.requireNonNull(createdAt);
        if (originalUrl.toString().length() > 2048 || originalUrl.toASCIIString().length() > 2048)
            throw new IllegalArgumentException("URL must not exceed 2048 characters");
        if (!isHttp(originalUrl)) throw new IllegalArgumentException("Only HTTP and HTTPS URLs are allowed");
        if (expiresAt != null && !expiresAt.isAfter(createdAt))
            throw new IllegalArgumentException("Expiration must be in the future");
    }

    public boolean isExpired(Clock clock) {
        return expiresAt != null && !expiresAt.isAfter(clock.instant());
    }

    private static boolean isHttp(URI uri) {
        return uri.isAbsolute() && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null;
    }
}
