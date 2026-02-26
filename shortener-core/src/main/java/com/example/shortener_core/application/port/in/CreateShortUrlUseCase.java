package com.example.shortener_core.application.port.in;

import com.example.shortener_core.domain.model.ShortUrl;

public interface CreateShortUrlUseCase {
    ShortUrl createShortUrl(String longUrl, Long ttl);
}
