package com.shortenerSoft.shortener_redirect.application.port;

import reactor.core.publisher.Mono;

public interface CacheCheckoutUseCase {
    Mono<String> checkout(String LongUrl);
}
