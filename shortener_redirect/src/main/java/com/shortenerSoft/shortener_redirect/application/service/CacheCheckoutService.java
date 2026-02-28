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

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
public class CacheCheckoutService implements CacheCheckoutUseCase {

    private static final long DEFAULT_TTL_IF_NOT_SPECIFIED = 30; // минут
    private static final long THIRTY_MINUTES_IN_MILLIS = 30 * 60 * 1000;
    private static final Logger log = LoggerFactory.getLogger(CacheCheckoutService.class);

    private final CachePort cachePort;
    private final CoreService coreService;

    public CacheCheckoutService(CachePort cachePort, CoreService coreService) {
        this.cachePort = cachePort;
        this.coreService = coreService;
    }

    @Override
    public Mono<String> checkout(String shortCode) {
        System.out.println("Контроллер зашёл в checkout");
        return cachePort.get(shortCode)
                .switchIfEmpty(
                        coreService.findLongUrlByShortCode(shortCode)
                                .flatMap(this::processUrlFromCore)
                                .doOnError(error ->
                                        log.error("Error fetching from core service for shortCode: {}", shortCode, error)
                                )
                                .onErrorResume(ex -> {
                                    if (ex instanceof WebClientResponseException.Gone) {
                                        log.warn("Код {} просрочен и удален в Core", shortCode);
                                        return Mono.error(new LinkExpiredException("URL has expired"));

                                    } else if (ex instanceof WebClientResponseException.NotFound) {
                                        log.warn("Код {} не найден в Core", shortCode);
                                        return Mono.empty();

                                    } else {
                                        log.error("Непредвиденная ошибка Core: {}", ex.getMessage());
                                        return Mono.error(ex); // Пропагируем ошибку дальше
                                    }
                                })
                );
    }

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
