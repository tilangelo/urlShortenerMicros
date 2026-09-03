package com.example.shortener_core.application.port.in;

import com.example.shortener_core.domain.model.ShortUrl;

import java.time.Instant;

public interface CreateShortUrlUseCase {
    ShortUrl createShortUrl(String longUrl, Instant expiration);
}
