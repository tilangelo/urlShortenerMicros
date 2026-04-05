package com.example.api_gateway.service;

import com.example.api_gateway.model.LinkPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkPolicyService {
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final JsonMapper jsonMapper;
    
    private static final String POLICY_KEY_PREFIX = "link:policy:";
    private static final String CORE_SERVICE_BASE_URL = "http://shortener-core:8080";
    
    public Mono<LinkPolicy> getPolicy(String shortcode) {
        String redisKey = POLICY_KEY_PREFIX + shortcode;
        
        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(json -> {
                    try {
                        LinkPolicy policy = jsonMapper.readValue(json, LinkPolicy.class);
                        log.debug("Retrieved policy from Redis for shortcode: {}", shortcode);
                        return Mono.just(policy);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse policy from Redis for shortcode: {}", shortcode, e);
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(
                    fetchPolicyFromCore(shortcode)
                            .flatMap(policy -> {
                                // Кэшируем политику для будущего использования
                                try {
                                    String json = jsonMapper.writeValueAsString(policy);
                                    redisTemplate.opsForValue()
                                            .set(redisKey, json, Duration.ofHours(24))
                                            .subscribe();
                                } catch (JsonProcessingException e) {
                                    log.error("Failed to serialize policy for caching", e);
                                }
                                return Mono.just(policy);
                            })
                );
    }
    
    private Mono<LinkPolicy> fetchPolicyFromCore(String shortcode) {
        String url = CORE_SERVICE_BASE_URL + "/internal/links/" + shortcode + "/policy";
        
        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(LinkPolicy.class)
                .doOnSuccess(policy -> log.debug("Fetched policy from core service for shortcode: {}", shortcode))
                .doOnError(error -> log.error("Failed to fetch policy from core service for shortcode: {}", shortcode, error))
                .onErrorResume(error -> Mono.empty());
    }


    // пока не используется
    public Mono<Boolean> policyExists(String shortcode) {
        String redisKey = POLICY_KEY_PREFIX + shortcode;
        
        return redisTemplate.hasKey(redisKey)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(true);
                    }
                    
                    // fallback: обращение к core сервису
                    String url = CORE_SERVICE_BASE_URL + "/internal/links/" + shortcode + "/policy-exists";
                    return webClientBuilder.build()
                            .get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(Boolean.class)
                            .defaultIfEmpty(false);
                });
    }
}
