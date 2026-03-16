package com.shortenerSoft.shortener_redirect.application.port;

import reactor.core.publisher.Mono;

import java.util.Optional;

public interface CachePort {

    Mono<String> get(String shortCode);

}
