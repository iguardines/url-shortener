package com.example.urlshortener.application;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

public interface RedirectCache {
    Optional<URI> get(String shortCode);

    void put(String shortCode, URI url, Duration ttl);

    void evict(String shortCode);
}
