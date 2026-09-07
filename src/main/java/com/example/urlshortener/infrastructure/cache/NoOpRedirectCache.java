package com.example.urlshortener.infrastructure.cache;

import com.example.urlshortener.application.RedirectCache;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * H2 resuelve los enlaces directamente, sin un servicio de cache externo.
 */
@Component
public class NoOpRedirectCache implements RedirectCache {
    public Optional<URI> get(String code) {
        return Optional.empty();
    }

    public void put(String code, URI url, Duration ttl) {
    }

    public void evict(String code) {
    }
}
