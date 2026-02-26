package com.shortenerSoft.shortener_redirect.infrastructure.adapter;

import com.shortenerSoft.shortener_redirect.application.port.CachePort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisCacheAdapter implements CachePort {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public RedisCacheAdapter(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }


    @Override
    public Mono<Boolean> save(String shortCode, String longUrl, Long ttl) {
        return reactiveRedisTemplate.opsForValue().set(
                buildKey(shortCode),
                longUrl,
                Duration.ofMillis(ttl));
    }

    public Mono<Boolean> saveWithDefaultTtl(String shortCode, String longUrl) {
        return reactiveRedisTemplate.opsForValue().set(
                buildKey(shortCode),
                longUrl,
                Duration.ofMinutes(30));
    }

    @Override
    public Mono<String> get(String shortCode) {
        return reactiveRedisTemplate.opsForValue().get(buildKey(shortCode));
    }

    @Override
    public Mono<Boolean> delete(String shortCode) {
        return reactiveRedisTemplate.opsForValue()
                .delete(buildKey(shortCode))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> exists(String shortCode) {
        return reactiveRedisTemplate.hasKey(buildKey(shortCode));
    }


    private String buildKey(String shortCode) {
        return "url:" + shortCode;
    }
}
