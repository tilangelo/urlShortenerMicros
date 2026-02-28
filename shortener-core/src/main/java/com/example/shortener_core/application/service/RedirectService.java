package com.example.shortener_core.application.service;

import com.example.shortener_core.api.dto.RedirectResponse;
import com.example.shortener_core.application.port.in.RedirectUseCase;
import com.example.shortener_core.application.port.out.CachePort;
import com.example.shortener_core.application.port.out.UrlRepositoryPort;
import com.example.shortener_core.common.exception.LinkExpiredException;
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
    public RedirectResponse redirect(String shortCode){

        // Поиск по бд шорткода и получение модели shortUrl
        ShortUrl shortUrl = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new NotFoundException("Ошибка поиска, не найден ШортКод: " + shortCode));


        System.out.println("shortCode: " + shortCode + " || LongUrl: " + shortUrl.getLongUrl());

        if(shortUrl.isExpired()){
            System.out.println("Удаление...");

            boolean deleted = repository.deleteByShortCode(shortCode);

            if (deleted) {
                System.out.println("Шорт код " + shortCode + " был успешно удалён!");
            } else {
                System.out.println("Не удалось удалить шорт код " + shortCode);
            }

            throw new LinkExpiredException("This url has expired");
        }

        System.out.println("Url has not expired" + shortUrl.getExpiresAt().toString());

        Instant expAt = shortUrl.getExpiresAt();

        Duration duration = Duration.between(Instant.now(), expAt);

        return new RedirectResponse(shortCode, shortUrl.getLongUrl(), duration.toMillis());
        //

    }
}
