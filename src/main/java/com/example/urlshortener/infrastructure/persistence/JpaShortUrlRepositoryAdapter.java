package com.example.urlshortener.infrastructure.persistence;

import com.example.urlshortener.application.ShortUrlRepository;
import com.example.urlshortener.domain.ShortUrl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.Optional;

@Repository
class JpaShortUrlRepositoryAdapter implements ShortUrlRepository {
    private final SpringDataShortUrlRepository delegate;

    JpaShortUrlRepositoryAdapter(SpringDataShortUrlRepository delegate) {
        this.delegate = delegate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShortUrl save(ShortUrl value) {
        var e = toEntity(value);
        return toDomain(delegate.saveAndFlush(e));
    }

    public Optional<ShortUrl> findByShortCode(String code) {
        return delegate.findByShortCode(code).map(this::toDomain);
    }

    public boolean existsByShortCode(String code) {
        return delegate.existsByShortCode(code);
    }

    public boolean incrementAccessCount(String code) {
        return delegate.increment(code) == 1;
    }

    public boolean deleteByShortCode(String code) {
        return delegate.deleteByShortCode(code) == 1;
    }

    private ShortUrlEntity toEntity(ShortUrl v) {
        var e = new ShortUrlEntity();
        e.id = v.id();
        e.shortCode = v.shortCode();
        e.originalUrl = v.originalUrl().toString();
        e.createdAt = v.createdAt();
        e.expiresAt = v.expiresAt();
        e.accessCount = v.accessCount();
        return e;
    }

    private ShortUrl toDomain(ShortUrlEntity e) {
        return new ShortUrl(e.id, e.shortCode, URI.create(e.originalUrl), e.createdAt, e.expiresAt, e.accessCount);
    }
}
