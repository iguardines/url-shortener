package com.example.urlshortener.application;

import com.example.urlshortener.domain.ShortUrl;

import java.util.Optional;

public interface ShortUrlRepository {
    ShortUrl save(ShortUrl shortUrl);

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    boolean incrementAccessCount(String shortCode);

    boolean deleteByShortCode(String shortCode);
}
