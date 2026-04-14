package com.example.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEndpointExchange {

    private final WebClient.Builder webClientBuilder;

    @Value("${gateway.auth.sso-endpoint}")
    private String ssoEndpoint;

    @Value("${gateway.auth.api-key-endpoint}")
    private String apiKeyEndpoint;


    public Mono<Boolean> corpSsoExchange(String token, String clientIp, Duration SSO_TIMEOUT){

        return webClientBuilder.build()
                .post()
                .uri(ssoEndpoint)
                .header("Authorization", "Bearer " + token)
                .header("X-Client-IP", clientIp)
                .retrieve()
                .bodyToMono(Boolean.class)

                // при пустом ответе false
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("SSO server returned empty body");
                    return Mono.just(false);
                }))
				
				.timeout(SSO_TIMEOUT)

                // логи
                .doOnSuccess(valid ->
                        log.debug("SSO validation result: {}", valid)
                )
                .doOnError(error -> {
                    if (error instanceof java.util.concurrent.TimeoutException) {
                        log.warn("SSO validation timeout for token: {}", token.substring(0, Math.min(10, token.length())));
                    } else {
                        log.error("SSO validation failed", error);
                    }
                })

                .onErrorReturn(false);

    }


    public Mono<Boolean> apiKeyExchange(String apiKey, String clientIp){

        return webClientBuilder.build()
                .post()
                .uri(apiKeyEndpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Client-IP", clientIp)
                .retrieve()
                .bodyToMono(Boolean.class)

                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("API key validation endpoint returned empty body");
                    return Mono.just(false);
                }))

                .doOnSuccess(valid ->
                        log.debug("API key validation result: {}", valid)
                )
                .doOnError(error ->
                        log.error("API key validation failed", error)
                )

                .onErrorReturn(false);

    }

}
