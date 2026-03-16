package com.example.shortener_core.infrastructure.Adapter;

import com.example.shortener_core.application.port.out.CachePort;
import com.example.shortener_core.domain.model.ShortUrlRedisSerializable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@Slf4j
public class RedisCacheAdapter implements CachePort {
    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper mapper;

    public RedisCacheAdapter(RedisTemplate<String, String> redisTemplate, JsonMapper mapper) {
        this.redisTemplate = redisTemplate;
        this.mapper = mapper;
    }


    @Override
    public boolean save(String shortCode, ShortUrlRedisSerializable serializable) {

        log.info("Сохранение hash в redis...");

        String json = mapper.writeValueAsString(serializable);

        return redisTemplate.opsForValue().setIfAbsent(
                buildKey(shortCode),
                json
        );
    }


    @Override
    public ShortUrlRedisSerializable get(String shortCode) {
        log.debug("Получение данных из redis");

        String value = redisTemplate.opsForValue().get(buildKey(shortCode));

        if (value == null) {
            return null;
        }

        return mapper.readValue(value, ShortUrlRedisSerializable.class);

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
