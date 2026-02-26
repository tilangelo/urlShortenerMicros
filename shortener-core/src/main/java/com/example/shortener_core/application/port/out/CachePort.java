package com.example.shortener_core.application.port.out;

import java.util.Optional;

public interface CachePort {
    boolean save(String shortCode, String longUrl, Long ttl);

    boolean saveWithDefaultTtl(String shortCode, String longUrl);

    Optional<String> get(String shortCode);

    boolean delete(String shortCode);

    boolean exists(String shortCode);
}
