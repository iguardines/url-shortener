package com.example.urlshortener.infrastructure.kafka;
import com.example.urlshortener.domain.ShortUrl;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.*;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaShortUrlEventPublisherTest {
    private final KafkaTemplate<String,String> kafka = mock(KafkaTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final KafkaShortUrlEventPublisher publisher = new KafkaShortUrlEventPublisher(kafka,mapper,"short-url-created.v1","short-url-visited.v1","url-shortener");
    private final ShortUrl link = new ShortUrl(42L,"abc123",URI.create("https://example.com"),Instant.parse("2026-09-08T12:00:00Z"),null,0);
    @BeforeEach void setup() { when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.completedFuture(null)); }
    @AfterEach void cleanup() { TransactionSynchronizationManager.clear(); }
    @Test void recordsUseCompatibleJsonAndSameStableUuid() throws Exception {
        publisher.created(link); publisher.visited(link,link.createdAt().plusSeconds(1));
        ArgumentCaptor<String> created = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> visited = ArgumentCaptor.forClass(String.class);
        String key = KafkaShortUrlEventPublisher.eventUrlId("url-shortener",42L).toString();
        verify(kafka).send(eq("short-url-created.v1"),eq(key),created.capture());
        verify(kafka).send(eq("short-url-visited.v1"),eq(key),visited.capture());
        JsonNode c = mapper.readTree(created.getValue()); JsonNode v = mapper.readTree(visited.getValue());
        assertThat(c.get("shortUrlId").asText()).isEqualTo(key);
        assertThat(v.get("shortUrlId").asText()).isEqualTo(key);
        assertThat(c.get("originalUrl").asText()).isEqualTo("https://example.com");
        assertThat(v.has("originalUrl")).isFalse();
        assertThat(c.get("occurredAt").asText()).isEqualTo("2026-09-08T12:00:00Z");
        assertThat(c.get("eventId").asText()).isNotEqualTo(v.get("eventId").asText());
    }
    @Test void separateDatabaseNamespaceAvoidsReusedIdCollision() {
        assertThat(KafkaShortUrlEventPublisher.eventUrlId("url-shortener",42L))
                .isNotEqualTo(KafkaShortUrlEventPublisher.eventUrlId("url-shortener-postgres-local",42L));
        assertThat(KafkaShortUrlEventPublisher.eventUrlId("url-shortener",42L))
                .isEqualTo(java.util.UUID.nameUUIDFromBytes("url-shortener:42".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    @Test void onlyPublishesAfterCommit() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        publisher.visited(link,link.createdAt());
        verifyNoInteractions(kafka);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafka).send(eq("short-url-visited.v1"),anyString(),anyString());
    }
    @Test void rollbackDoesNotPublish() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        publisher.visited(link,link.createdAt());
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(kafka);
    }
    @Test void synchronousBrokerFailureDoesNotFailCommittedBusinessOperation() {
        when(kafka.send(anyString(),anyString(),anyString())).thenThrow(new IllegalStateException("broker unavailable"));
        assertThatCode(() -> publisher.created(link)).doesNotThrowAnyException();
    }
}
