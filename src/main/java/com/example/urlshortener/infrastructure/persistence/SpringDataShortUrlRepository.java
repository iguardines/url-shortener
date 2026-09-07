package com.example.urlshortener.infrastructure.persistence;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface SpringDataShortUrlRepository extends JpaRepository<ShortUrlEntity, Long> {
    Optional<ShortUrlEntity> findByShortCode(String code);

    boolean existsByShortCode(String code);

    @Modifying
    @Query("update ShortUrlEntity s set s.accessCount = s.accessCount + 1 where s.shortCode = :code")
    int increment(@Param("code") String code);

    long deleteByShortCode(String code);
}
