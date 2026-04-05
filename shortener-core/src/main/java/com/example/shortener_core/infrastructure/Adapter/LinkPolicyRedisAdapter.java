package com.example.shortener_core.infrastructure.Adapter;

import com.example.shortener_core.application.port.out.LinkPolicyCachePort;
import com.example.shortener_core.domain.model.LinkPolicyRedis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkPolicyRedisAdapter implements LinkPolicyCachePort {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final JsonMapper jsonMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    
    private static final String POLICY_KEY_PREFIX = "link:policy:";
    private static final long DEFAULT_TTL_HOURS = 24;
    
    @Override
    public void savePolicy(String shortcode, LinkPolicyRedis policy) {
        try {
            String key = buildKey(shortcode);
            String json = jsonMapper.writeValueAsString(policy);
            
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Saved link policy to Redis for shortcode: {}", shortcode);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize link policy for shortcode: {}", shortcode, e);
            throw new RuntimeException("Failed to serialize link policy", e);
        }
    }
    
    @Override
    public Optional<LinkPolicyRedis> getPolicy(String shortcode) {
        try {
            String key = buildKey(shortcode);
            String json = redisTemplate.opsForValue().get(key);
            
            if (json == null) {
                log.debug("No policy found in Redis for shortcode: {}", shortcode);
                return Optional.empty();
            }
            
            LinkPolicyRedis policy = jsonMapper.readValue(json, LinkPolicyRedis.class);
            log.debug("Retrieved link policy from Redis for shortcode: {}", shortcode);
            return Optional.of(policy);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize link policy for shortcode: {}", shortcode, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void deletePolicy(String shortcode) {
        String key = buildKey(shortcode);
        redisTemplate.delete(key);
        log.debug("Deleted link policy from Redis for shortcode: {}", shortcode);
    }
    
    @Override
    public boolean existsPolicy(String shortcode) {
        String key = buildKey(shortcode);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    private String buildKey(String shortcode) {
        return POLICY_KEY_PREFIX + shortcode;
    }
}
