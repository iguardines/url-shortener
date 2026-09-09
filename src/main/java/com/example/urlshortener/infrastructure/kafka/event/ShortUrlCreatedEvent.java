package com.example.urlshortener.infrastructure.kafka.event;
import java.time.Instant;
import java.util.UUID;
public record ShortUrlCreatedEvent(UUID eventId, UUID shortUrlId, String shortCode, String originalUrl, Instant occurredAt) {}
