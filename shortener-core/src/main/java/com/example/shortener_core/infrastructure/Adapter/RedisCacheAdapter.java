package com.example.shortener_core.infrastructure.Adapter;

import com.example.shortener_core.application.port.out.CachePort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisCacheAdapter implements CachePort {
    private final RedisTemplate<String, String> redisTemplate;

    public RedisCacheAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }



    @Override
    public boolean save(String shortCode, String longUrl, Long ttl) {
        return redisTemplate.opsForValue().setIfAbsent(
                buildKey(shortCode),
                longUrl,
                Duration.ofMinutes(ttl));
    }

    @Override
    public boolean saveWithDefaultTtl(String shortCode, String longUrl) {
        return redisTemplate.opsForValue().setIfAbsent(
                buildKey(shortCode),
                longUrl,
                Duration.ofMinutes(30));
    }

    @Override
    public Optional<String> get(String shortCode) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(
                buildKey(shortCode))
        );
    }

    @Override
    public boolean delete(String shortCode) {
        return Boolean.TRUE.equals(redisTemplate.delete(buildKey(shortCode)));
    }

    @Override
    public boolean exists(String shortCode) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(shortCode)));
    }

    private String buildKey(String shortCode) {
        return "url:" + shortCode;
    }
}
