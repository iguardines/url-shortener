package com.example.urlshortener.infrastructure.kafka;

import com.example.urlshortener.application.ShortUrlEventPublisher;
import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.infrastructure.kafka.event.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(name="app.events.enabled", havingValue="true", matchIfMissing=true)
public class KafkaShortUrlEventPublisher implements ShortUrlEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaShortUrlEventPublisher.class);
    private final KafkaTemplate<String,String> kafka;
    private final ObjectMapper mapper;
    private final String createdTopic;
    private final String visitedTopic;
    private final String idNamespace;

    public KafkaShortUrlEventPublisher(KafkaTemplate<String,String> kafka, ObjectMapper mapper,
            @Value("${app.events.topics.created}") String createdTopic,
            @Value("${app.events.topics.visited}") String visitedTopic,
            @Value("${app.events.id-namespace:url-shortener}") String idNamespace) {
        if (idNamespace == null || idNamespace.isBlank()) throw new IllegalArgumentException("Event ID namespace must not be blank");
        this.idNamespace=idNamespace;
        this.kafka=kafka; this.mapper=mapper; this.createdTopic=createdTopic; this.visitedTopic=visitedTopic;
    }
    static UUID eventUrlId(String namespace, Long id) {
        return UUID.nameUUIDFromBytes((namespace + ":" + java.util.Objects.requireNonNull(id)).getBytes(StandardCharsets.UTF_8));
    }
    @Override public void created(ShortUrl link) {
        UUID id=eventUrlId(idNamespace, link.id());
        publishAfterCommit(createdTopic, id, new ShortUrlCreatedEvent(UUID.randomUUID(),id,link.shortCode(),link.originalUrl().toString(),link.createdAt()));
    }
    @Override public void visited(ShortUrl link, Instant at) {
        UUID id=eventUrlId(idNamespace, link.id());
        publishAfterCommit(visitedTopic, id, new ShortUrlVisitedEvent(UUID.randomUUID(),id,link.shortCode(),at));
    }
    private void publishAfterCommit(String topic, UUID key, Object event) {
        Runnable publish = () -> {
            try {
                kafka.send(topic,key.toString(),mapper.writeValueAsString(event)).whenComplete((result,failure) -> {
                    if (failure!=null) log.error("Kafka publication failed topic={} shortUrlId={}; outbox/replay required",topic,key,failure);
                });
            } catch (Exception failure) {
                log.error("Kafka publication failed topic={} shortUrlId={}; database change already committed",topic,key,failure);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { publish.run(); }
            });
        } else publish.run();
    }
}
