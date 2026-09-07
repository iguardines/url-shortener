package com.example.urlshortener.application;

import com.example.urlshortener.application.exception.*;
import com.example.urlshortener.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.*;
import java.util.regex.Pattern;

@Service
public class ShortUrlService {
    private static final Logger log = LoggerFactory.getLogger(ShortUrlService.class);
    private static final Pattern ALIAS = Pattern.compile("[A-Za-z0-9_-]{4,32}");
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);
    private final ShortUrlRepository repository;
    private final RedirectCache cache;
    private final ShortCodeGenerator generator;
    private final Clock clock;

    public ShortUrlService(ShortUrlRepository repository, RedirectCache cache, ShortCodeGenerator generator, Clock clock) {
        this.repository = repository;
        this.cache = cache;
        this.generator = generator;
        this.clock = clock;
    }

    // Each insert has its own transaction so a collision does not poison the retry.
    public ShortUrl create(CreateShortUrlCommand command) {
        Instant now = clock.instant();
        if (command.expiresAt() != null && !command.expiresAt().isAfter(now))
            throw new IllegalArgumentException("Expiration must be in the future");
        String alias = normalizeAlias(command.customAlias());
        if (alias != null) {
            if (repository.existsByShortCode(alias)) throw new DuplicateShortCodeException(alias);
            return persist(command, alias, now, false);
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String code = generator.generate();
            if (repository.existsByShortCode(code)) continue;
            try {
                return persist(command, code, now, true);
            } catch (DataIntegrityViolationException collision) {
                log.warn("Generated short-code collision; retrying attempt={}", attempt);
            }
        }
        throw new IllegalStateException("Could not allocate a unique short code");
    }

    @Transactional
    public URI resolve(String code) {
        var cached = cache.get(code);
        if (cached.isPresent()) {
            repository.incrementAccessCount(code);
            return cached.get();
        }
        ShortUrl link = getActive(code);
        repository.incrementAccessCount(code);
        cache.put(code, link.originalUrl(), cacheTtl(link));
        return link.originalUrl();
    }

    @Transactional(readOnly = true)
    public ShortUrl get(String code) {
        return repository.findByShortCode(code).orElseThrow(() -> new ShortUrlNotFoundException(code));
    }

    @Transactional
    public void delete(String code) {
        if (!repository.deleteByShortCode(code)) throw new ShortUrlNotFoundException(code);
        cache.evict(code);
        log.info("Deleted short URL code={}", code);
    }

    private ShortUrl getActive(String code) {
        ShortUrl link = get(code);
        if (link.isExpired(clock)) {
            cache.evict(code);
            throw new ShortUrlExpiredException(code);
        }
        return link;
    }

    private ShortUrl persist(CreateShortUrlCommand c, String code, Instant now, boolean generated) {
        try {
            ShortUrl saved = repository.save(new ShortUrl(null, code, c.originalUrl(), now, c.expiresAt(), 0));
            log.info("Created short URL code={} customAlias={} expires={}", code, !generated, c.expiresAt() != null);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            if (!generated) throw new DuplicateShortCodeException(code);
            throw ex;
        }
    }

    private Duration cacheTtl(ShortUrl link) {
        if (link.expiresAt() == null) return DEFAULT_CACHE_TTL;
        return Duration.between(clock.instant(), link.expiresAt()).compareTo(DEFAULT_CACHE_TTL) < 0 ? Duration.between(clock.instant(), link.expiresAt()) : DEFAULT_CACHE_TTL;
    }

    private String normalizeAlias(String alias) {
        if (alias == null || alias.isBlank()) return null;
        String trimmed = alias.trim();
        if (java.util.Set.of("actuator", "swagger-ui", "error").contains(trimmed))
            throw new IllegalArgumentException("Alias is reserved");
        if (!ALIAS.matcher(trimmed).matches())
            throw new IllegalArgumentException("Alias must contain 4-32 letters, digits, '_' or '-'");
        return trimmed;
    }
}
