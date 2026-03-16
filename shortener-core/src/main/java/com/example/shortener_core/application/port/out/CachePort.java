package com.example.shortener_core.application.port.out;

import com.example.shortener_core.domain.model.ShortUrlRedisSerializable;

public interface CachePort {
    boolean save(String shortCode, ShortUrlRedisSerializable shortUrlRedisSerializable);

    ShortUrlRedisSerializable get(String shortCode);

    boolean delete(String shortCode);

    boolean exists(String shortCode);
}
