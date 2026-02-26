package com.example.shortener_core.application.port.out;

import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.infrastructure.persistence.entity.UrlEntity;

import java.util.Optional;

public interface UrlRepositoryPort {
    ShortUrl save(ShortUrl shortUrl);

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    void incrementClickCount(String shortCode);

    UrlEntity findByLongUrl(String longUrl);

    boolean deleteByShortCode(String shortCode);
}
