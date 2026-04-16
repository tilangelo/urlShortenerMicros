package com.example.api_gateway.service;

import com.example.api_gateway.model.LinkPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private static final Duration WEBCLIENT_TIMEOUT = Duration.ofSeconds(5);

    @Value("${gateway.core-service.base-url}")
    private String coreServiceBaseUrl;

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
                                        return redisTemplate.opsForValue()
                                                .set(redisKey, json, Duration.ofHours(24))
                                                .timeout(Duration.ofSeconds(2))
                                                .onErrorResume(throwable -> {
                                                    log.warn("Failed to cache policy for shortcode: {}", shortcode, throwable);
                                                    return Mono.empty();
                                                })
                                                .then(Mono.just(policy));
                                    } catch (JsonProcessingException e) {
                                        log.error("Failed to serialize policy for caching", e);
                                        return Mono.just(policy);
                                    }
                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                    return Mono.empty();
                                }))
                );
    }

    private Mono<LinkPolicy> fetchPolicyFromCore(String shortcode) {
        String url = coreServiceBaseUrl + "/internal/links/" + shortcode + "/policy";

        WebClient webClient = webClientBuilder
                .clone()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        return webClient
                .get()
                .uri(url)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("Unknown error")
                                .map(body -> new RuntimeException("Core service error: " + response.statusCode() + " - " + body))
                )
                .bodyToMono(LinkPolicy.class)
                .timeout(WEBCLIENT_TIMEOUT)
                .doOnSuccess(policy -> log.debug("Fetched policy from core service for shortcode: {}", shortcode))
                .doOnError(error -> {
                    if (error instanceof java.util.concurrent.TimeoutException) {
                        log.warn("Timeout fetching policy from core service for shortcode: {}", shortcode);
                    } else {
                        log.error("Failed to fetch policy from core service for shortcode: {}", shortcode, error);
                    }
                })
                .onErrorResume(error -> Mono.empty());
    }


    // Проверка существования ссылки в Redis
    public Mono<Boolean> shortcodeExistsInRedis(String shortcode) {
        String linkKey = "url:" + shortcode;

        return redisTemplate.hasKey(linkKey)
                .doOnSuccess(exists -> {
                    if (exists) {
                        log.debug("Shortcode {} found in Redis", shortcode);
                    } else {
                        log.warn("Shortcode {} not found in Redis", shortcode);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error checking shortcode existence in Redis for: {}", shortcode, error);
                    return Mono.just(false);
                });
    }
}