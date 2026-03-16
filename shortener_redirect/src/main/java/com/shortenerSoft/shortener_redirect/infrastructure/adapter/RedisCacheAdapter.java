package com.shortenerSoft.shortener_redirect.infrastructure.adapter;

import com.shortenerSoft.shortener_redirect.application.exception.LinkDoesNotExistException;
import com.shortenerSoft.shortener_redirect.application.port.CachePort;
import com.shortenerSoft.shortener_redirect.infrastructure.model.ShortUrlRedisSerializable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisCacheAdapter implements CachePort {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public RedisCacheAdapter(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }


    @Override
    public Mono<String> get(String shortCode) {
        return reactiveRedisTemplate.opsForValue().get(buildKey(shortCode))
                .switchIfEmpty(Mono.error(new LinkDoesNotExistException("Ссылка не существует")));
    }


    private String buildKey(String shortCode) {
        return "url:" + shortCode;
    }

}
