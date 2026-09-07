package com.example.urlshortener.application;

import java.net.URI;
import java.time.Instant;

public record CreateShortUrlCommand(URI originalUrl, String customAlias, Instant expiresAt) {
}
