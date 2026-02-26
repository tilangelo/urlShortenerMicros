package com.shortenerSoft.shortener_redirect.application.port;

import reactor.core.publisher.Mono;

import java.util.Optional;

public interface CachePort {
    Mono<Boolean> save(String shortCode, String longUrl, Long ttl);

    Mono<Boolean> saveWithDefaultTtl(String shortCode, String longUrl);

    Mono<String> get(String shortCode);

    Mono<Boolean> delete(String shortCode);

    Mono<Boolean> exists(String shortCode);
}
