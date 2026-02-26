package com.example.shortener_core.application.service;

import com.example.shortener_core.api.dto.RedirectResponse;
import com.example.shortener_core.application.port.in.RedirectUseCase;
import com.example.shortener_core.application.port.out.CachePort;
import com.example.shortener_core.application.port.out.UrlRepositoryPort;
import com.example.shortener_core.common.exception.NotFoundException;
import com.example.shortener_core.domain.model.ShortUrl;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;


@Service
public class RedirectService implements RedirectUseCase {

    private final CachePort cachePort;
    private final UrlRepositoryPort repository;

    public RedirectService(CachePort cachePort,
                           UrlRepositoryPort repository) {
        this.cachePort = cachePort;
        this.repository = repository;
    }

    @Override
    public RedirectResponse redirect(String shortCode) throws Exception {

        // Поиск по бд шорткода и получение модели shortUrl
        ShortUrl shortUrl = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new NotFoundException("Ошибка поиска, не найден ШортКод: " + shortCode));


        if(shortUrl.isExpired()){
            repository.deleteByShortCode(shortCode);
            throw new Exception("This url has expired");
        }

        Instant expAt = shortUrl.getExpiresAt();

        Duration duration = Duration.between(Instant.now(), expAt);

        return new RedirectResponse(shortCode, shortUrl.getLongUrl(), duration.toMillis());
        //

    }
}
