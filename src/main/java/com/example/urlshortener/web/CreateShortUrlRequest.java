package com.example.urlshortener.web;

import jakarta.validation.constraints.*;

import java.net.URI;
import java.time.Instant;

public record CreateShortUrlRequest(@NotNull URI url, @Pattern(regexp = "[A-Za-z0-9_-]{4,32}") String customAlias,
                                    @Future Instant expiresAt) {
}
