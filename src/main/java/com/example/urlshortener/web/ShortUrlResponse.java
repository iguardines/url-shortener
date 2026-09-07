package com.example.urlshortener.web;

import com.example.urlshortener.domain.ShortUrl;

import java.time.Instant;

public record ShortUrlResponse(String shortCode, String shortUrl, String originalUrl, Instant createdAt,
                               Instant expiresAt, long accessCount) {
    static ShortUrlResponse from(ShortUrl link, String baseUrl) {
        return new ShortUrlResponse(link.shortCode(), baseUrl + "/" + link.shortCode(), link.originalUrl().toString(), link.createdAt(), link.expiresAt(), link.accessCount());
    }
}
