package com.example.urlshortener.infrastructure.kafka;
import com.example.urlshortener.application.ShortUrlEventPublisher;
import com.example.urlshortener.domain.ShortUrl;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="app.events.enabled",havingValue="false")
public class DisabledShortUrlEventPublisher implements ShortUrlEventPublisher {
    @Override public void created(ShortUrl link) {}
    @Override public void visited(ShortUrl link, Instant occurredAt) {}
}
