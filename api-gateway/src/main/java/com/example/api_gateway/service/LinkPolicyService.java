package com.example.api_gateway.service;

import com.example.api_gateway.model.LinkPolicy;
import com.example.api_gateway.model.exception.CacheServiceException;
import com.example.api_gateway.model.exception.CoreServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class LinkPolicyService {

    private static final String NO_POLICY_MARKER = "__NO_POLICY__";
    private static final Duration NEGATIVE_CACHE_TTL = Duration.ofSeconds(30);
    private static final Duration CACHE_WRITE_TIMEOUT = Duration.ofSeconds(2);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;
    private final WebClient coreWebClient;

    public LinkPolicyService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            JsonMapper jsonMapper,
            @Qualifier("coreWebClient") WebClient coreWebClient
    ) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
        this.coreWebClient = coreWebClient;
    }

    private static final String POLICY_KEY_PREFIX = "link:policy:";
    private static final Duration WEBCLIENT_TIMEOUT = Duration.ofSeconds(5);

    public Mono<LinkPolicy> getPolicy(String shortcode) {
        String redisKey = POLICY_KEY_PREFIX + shortcode;

        return redisTemplate.opsForValue().get(redisKey)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .doOnError(error ->
                        log.error(
                                "Failed to read cached policy for shortcode: {}",
                                shortcode,
                                error
                        )
                )
                .onErrorMap(error ->
                        new CacheServiceException(
                                "Failed to read policy from Redis",
                                error
                        )
                )
                .flatMap(cachedJson -> {
                    if(cachedJson.isEmpty()){
                        return fetchAndCachePolicy(shortcode, redisKey);
                    }

                    String json = cachedJson.get();

                    if(NO_POLICY_MARKER.equals(json)){
                        return Mono.empty();
                    }

                    return parseCachedPolicy(json, shortcode, redisKey)
                            // вернёт empty если произошла ошибка парсинга json
                            .switchIfEmpty(Mono.defer(() -> fetchAndCachePolicy(shortcode, redisKey)));
                });
    }

    private Mono<LinkPolicy> fetchAndCachePolicy(
            String shortcode,
            String redisKey
    ) {
        return fetchPolicyFromCore(shortcode)
                .flatMap(policy -> {
                    // Кэшируем политику для будущего использования
                    try {
                        String json = jsonMapper.writeValueAsString(policy);
                        return redisTemplate.opsForValue()
                                .set(redisKey, json, Duration.ofHours(24))
                                .timeout(Duration.ofSeconds(2))
                                .onErrorResume(throwable -> {
                                    log.error("Failed to cache policy for shortcode: {}", shortcode, throwable);
                                    return Mono.empty();
                                })
                                .then(Mono.just(policy));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize policy for caching", e);
                        return Mono.just(policy);
                    }
                })
                .switchIfEmpty(// отрицательный кэш: политики нет, ставим маркер
                        Mono.defer(() ->
                                cacheNoPolicy(shortcode, redisKey)
                        )
                );
    }

    private Mono<LinkPolicy> parseCachedPolicy(
            String json,
            String shortcode,
            String redisKey
    ) {

        try {
            LinkPolicy policy = jsonMapper.readValue(json, LinkPolicy.class);
            return Mono.just(policy);
        } catch (JsonProcessingException error) {
            log.error(
                    "Failed to parse cached policy for shortcode: {}",
                    shortcode,
                    error
            );

            // если исключение при парсе - удаление записи и
            // поиск её в core в вызывающем методе
            return redisTemplate.delete(redisKey)
                    .doOnSuccess(deleted ->
                            log.debug(
                                    "Corrupted cached policy deleted: {}",
                                    deleted
                            )
                    )
                    .doOnError(deleteError ->
                            log.error(
                                    "Failed to delete corrupted policy: {}",
                                    shortcode,
                                    deleteError
                            )
                    )
                    .onErrorResume(deleteError -> Mono.empty())
                    // Сообщаем внешнему switchIfEmpty, что надо обратиться к Core
                    .then(Mono.<LinkPolicy>empty());
        }
    }

    private Mono<LinkPolicy> fetchPolicyFromCore(String shortcode) {

        return coreWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/links/{shortcode}/policy")
                        .build(shortcode))
                .exchangeToMono(response -> {
                    HttpStatusCode status = response.statusCode();

                    if (status.value() == 404) {
                        return response.releaseBody().then(Mono.empty());
                    }

                    // Если core не принял credentials от gateway
                    if(status.value() == 401 || status.value() == 403) {
                        return response.releaseBody()
                                .then(Mono.error(
                                        new CoreServiceException(
                                                "Core rejected gateway service credentials " +
                                                        status
                                        )
                                ));
                    }

                    // Обработка 4xx
                    if (status.is4xxClientError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("Empty error body")
                                .flatMap(body -> Mono.error(
                                        new CoreServiceException(
                                                "Core rejected internal request " +
                                                        status + ": " + body
                                        )
                                ));
                    }

                    // Обработка 5xx
                    if (status.is5xxServerError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("Empty error body")
                                .flatMap(body -> Mono.error(
                                        new CoreServiceException(
                                                "Core service failed " +
                                                        status + ": " + body
                                        )
                                ));
                    }

                    // Упешный ответ (2xx) Десериализуем в LinkPolicy
                    if (status.is2xxSuccessful()) {
                        return response.bodyToMono(LinkPolicy.class)
                                .switchIfEmpty(Mono.error(
                                        new CoreServiceException(
                                                "Core returned successful response without policy"
                                        )
                                ));
                    }

                    // Любой другой статус
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("Empty response body")
                            .flatMap(body -> Mono.error(
                                    new CoreServiceException(
                                            "Unexpected core response " + status + ": " + body
                                    )
                            ));
                })

                .timeout(WEBCLIENT_TIMEOUT)

                .onErrorMap(
                        WebClientRequestException.class,
                        error -> new CoreServiceException(
                                "Failed to connect to core service", error
                        )
                )

                // 4. НАБЛЮДАТЕЛЬ: Логирование успеха (или пустоты при 404)
                .doOnSuccess(policy -> {
                    if (policy != null) {
                        log.debug("Fetched policy from core service for shortcode: {}", shortcode);
                    } else {
                        log.debug("Policy not found (404) for shortcode: {}", shortcode);
                    }
                })

                // 5. НАБЛЮДАТЕЛЬ: Логирование ошибок
                .doOnError(error -> {
                    if (error instanceof java.util.concurrent.TimeoutException) {
                        log.warn("Timeout fetching policy from core service for shortcode: {}", shortcode);
                    } else if (error instanceof CoreServiceException) {
                        log.error("Core service is unavailable for shortcode: {}", shortcode, error);
                    } else {
                        log.error("Failed to fetch policy from core service for shortcode: {}", shortcode, error);
                    }
                });
    }

    private Mono<LinkPolicy> cacheNoPolicy(
            String shortcode,
            String redisKey
    ) {
        return Mono.defer(() ->
                        redisTemplate.opsForValue().setIfAbsent(
                                redisKey,
                                NO_POLICY_MARKER,
                                NEGATIVE_CACHE_TTL
                        )
                )
                .timeout(CACHE_WRITE_TIMEOUT)
                .doOnNext(saved -> {
                    if (!saved) {
                        log.debug(
                                "Negative marker was not written because key already exists: {}",
                                shortcode
                        );
                    }
                })
                .then(Mono.<LinkPolicy>empty())
                .onErrorResume(error -> {
                    log.error(
                            "Failed to cache missing policy for shortcode: {}",
                            shortcode,
                            error
                    );

                    return Mono.empty();
                });
    }


    // Проверка существования ссылки в Redis
    public Mono<Boolean> shortcodeExistsInRedis(String shortcode) {
        String linkKey = "url:" + shortcode;

        return redisTemplate.hasKey(linkKey)
                .doOnSuccess(exists -> {
                    if (exists) {
                        log.debug("Shortcode {} found in Redis", shortcode);
                    }
                })
                .doOnError(error ->
                        log.error(
                                "Failed to check shortcode in Redis: {}",
                                shortcode,
                                error
                        )
                )
                .onErrorMap(error ->
                        new CacheServiceException(
                                "Redis is unavailable", error
                        )
                );
    }
}
