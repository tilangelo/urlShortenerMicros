package com.shortenerSoft.shortener_redirect.application.service;

import com.shortenerSoft.shortener_redirect.application.exception.LinkExpiredException;
import com.shortenerSoft.shortener_redirect.application.port.CacheCheckoutUseCase;
import com.shortenerSoft.shortener_redirect.application.port.CachePort;
import com.shortenerSoft.shortener_redirect.infrastructure.model.ShortUrlRedisSerializable;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

@Service
@Slf4j
public class CacheCheckoutService implements CacheCheckoutUseCase {

    @Value("${redirect.empty.uri:localhost}")
    private String redirectEmptyUri;

    private final JsonMapper mapper;
    private final CachePort cachePort;

    public CacheCheckoutService(JsonMapper mapper, CachePort cachePort) {
        this.mapper = mapper;
        this.cachePort = cachePort;
    }



    @Override
    public Mono<String> checkout(String shortCode) {
        log.info("Checking out shortCode {}", shortCode);

        return cachePort.get(shortCode)
                .log("CACHE CHECKOUT")
                // если ссылки нет или она просрочилась - вернет дефолтный юрл
                .defaultIfEmpty(redirectEmptyUri)
                .flatMap(json -> {
                    try {
                        ShortUrlRedisSerializable ser =
                                mapper.readValue(json, ShortUrlRedisSerializable.class);

                        if(Instant.now().isAfter(ser.getExpireAt())) {
                            log.warn("Ссылка к которой обращаются устарела");
                            return Mono.error(new LinkExpiredException("Ссылка устарела"));
                        }

                        return Mono.just(ser.getLongUrl());

                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                // возвращает 500 если ниже вернули ошибку
                .onErrorResume(ex -> {
                    log.error("Ошибка при получении элемента из redis: {}", ex.getMessage());
                    return Mono.error(ex);
        });

    }



}
