package com.example.urlshortener.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "short_urls")
class ShortUrlEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "short_code", nullable = false, unique = true, length = 32)
    String shortCode;
    @Column(name = "original_url", nullable = false, length = 2048)
    String originalUrl;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "expires_at")
    Instant expiresAt;
    @Column(name = "access_count", nullable = false)
    long accessCount;

    protected ShortUrlEntity() {
    }
}
