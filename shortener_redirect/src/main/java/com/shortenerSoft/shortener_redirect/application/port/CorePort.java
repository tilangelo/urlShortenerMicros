package com.shortenerSoft.shortener_redirect.application.port;

import com.shortenerSoft.shortener_redirect.api.dto.ShortenResponse;
import reactor.core.publisher.Mono;

public interface CorePort {
    Mono<ShortenResponse> findLongUrlByShortCode(String shortCode);
}
