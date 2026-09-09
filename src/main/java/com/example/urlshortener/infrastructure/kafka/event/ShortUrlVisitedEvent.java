package com.example.urlshortener.infrastructure.kafka.event;
import java.time.Instant;
import java.util.UUID;
public record ShortUrlVisitedEvent(UUID eventId, UUID shortUrlId, String shortCode, Instant occurredAt) {}
