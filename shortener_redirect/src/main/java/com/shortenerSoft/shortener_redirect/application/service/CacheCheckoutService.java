package com.shortenerSoft.shortener_redirect.application.service;

import com.shortenerSoft.shortener_redirect.api.dto.ShortenResponse;
import com.shortenerSoft.shortener_redirect.application.exception.LinkExpiredException;
import com.shortenerSoft.shortener_redirect.application.port.CacheCheckoutUseCase;
import com.shortenerSoft.shortener_redirect.application.port.CachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class CacheCheckoutService implements CacheCheckoutUseCase {

    private static final long DEFAULT_TTL_IF_NOT_SPECIFIED = 30; // минут
    private static final long THIRTY_MINUTES_IN_MILLIS = 30 * 60 * 1000;
    private static final Logger log = LoggerFactory.getLogger(CacheCheckoutService.class);

    private static final String REDIRECT_EMPTY_URI = "https://habr.com/ru/feed/";

    private final CachePort cachePort;

    public CacheCheckoutService(CachePort cachePort) {
        this.cachePort = cachePort;
    }

    @Override
    public Mono<String> checkout(String shortCode) {
        System.out.println("Контроллер зашёл в checkout");
        return cachePort.get(shortCode)
                .log("CACHE CHECKOUT")
                .defaultIfEmpty(REDIRECT_EMPTY_URI)
                .onErrorResume(ex -> {
            if (ex instanceof WebClientResponseException.Gone) {
                log.warn("Код {} просрочен и удален в Core", shortCode);
                return Mono.error(new LinkExpiredException("URL has expired"));

            } else {
                log.error("Непредвиденная ошибка Core: {}", ex.getMessage());
                return Mono.error(ex);
            }
        });
    }

    @Deprecated
    private Mono<String> processUrlFromCore(ShortenResponse response) {
        if (response == null || response.getLongUrl() == null) {
            return Mono.empty();
        }

        // Проверка на отрицательный или нулевой TTL
        if (response.getTtl() <= 0) {
            log.warn("Received non-positive TTL ({}) for shortCode, returning URL without caching", response.getTtl());
            return Mono.just(response.getLongUrl());
        }


        long ttlToUse;
        if(response.getTtl() <= THIRTY_MINUTES_IN_MILLIS) {
            ttlToUse = response.getTtl();

            // Если TTL не указан или больше 30 минут, используем стандартный TTL
        }else {
            ttlToUse = DEFAULT_TTL_IF_NOT_SPECIFIED * 60 * 1000;
        }


        return cachePort.save(response.getShortCode(), buildKey(response.getLongUrl()), ttlToUse)
                .thenReturn(response.getLongUrl());
    }

    private String buildKey(String longUrl) {
        // Здесь можно добавить логику построения ключа, если нужно
        return longUrl;
    }
}
