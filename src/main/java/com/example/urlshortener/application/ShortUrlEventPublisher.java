package com.example.urlshortener.application;
import com.example.urlshortener.domain.ShortUrl;
import java.time.Instant;

public interface ShortUrlEventPublisher {
    void created(ShortUrl link);
    void visited(ShortUrl link, Instant occurredAt);
}
