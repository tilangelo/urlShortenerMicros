package com.example.api_gateway.service;

import com.example.api_gateway.model.LinkPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthValidationService {
    
    private final WebClient.Builder webClientBuilder;
    
    public Mono<Boolean> validateAuth(String authType, LinkPolicy.AuthConfig authConfig, 
                                     String authHeader, String clientIp) {
        if (authType == null || authType.equals("null") || authType.isEmpty()) {
            return Mono.just(true); // Аутентификация не требуется
        }
        
        switch (authType) {
            case "corporate_sso":
                return validateCorporateSso(authConfig, authHeader);
            case "api_key":
                return validateApiKey(authConfig, authHeader);
            case "jwt":
                return validateJwt(authConfig, authHeader);
            case "basic":
                return validateBasic(authConfig, authHeader);
            default:
                log.warn("Unknown auth type: {}", authType);
                return Mono.just(false);
        }
    }
    
    private Mono<Boolean> validateCorporateSso(LinkPolicy.AuthConfig config, String authHeader) {
        if (config == null || config.getSso_endpoint() == null) {
            log.error("SSO endpoint not configured");
            return Mono.just(false);
        }
        
        // Извлекаем токен из заголовка Authorization
        String token = extractToken(authHeader);
        if (token == null) {
            return Mono.just(false);
        }
        
        return webClientBuilder.build()
                .post()
                .uri(config.getSso_endpoint())
                .header("Authorization", "Bearer " + token)
                .header("X-Client-IP", getClientIp())
                .retrieve()
                .bodyToMono(Boolean.class)
                .defaultIfEmpty(false)
                .doOnSuccess(valid -> log.debug("SSO validation result: {}", valid))
                .doOnError(error -> log.error("SSO validation failed", error))
                .onErrorReturn(false);
    }
    
    private Mono<Boolean> validateApiKey(LinkPolicy.AuthConfig config, String authHeader) {
        if (config == null || config.getApi_key_header() == null) {
            log.error("API key header not configured");
            return Mono.just(false);
        }
        
        // Пока что просто проверяем наличие заголовка
        // Потом нужно проверять по базе данных или внешнему сервису
        String apiKey = extractApiKey(authHeader, config.getApi_key_header());
        return Mono.just(apiKey != null && !apiKey.isEmpty());
    }
    
    private Mono<Boolean> validateJwt(LinkPolicy.AuthConfig config, String authHeader) {
        if (config == null || config.getJwt_secret_key() == null) {
            log.error("JWT secret not configured");
            return Mono.just(false);
        }
        
        String token = extractToken(authHeader);
        if (token == null) {
            return Mono.just(false);
        }
        
        // Валидация JWT была бы здесь
        // Пока что просто проверяем наличие токена и его срок действия
        try {
            // упрощённая проверка - потом использовать правильную валидацию JWT
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Mono.just(false);
            }
            
            // Проверяем, не истёк ли срок действия токена
            // Потом нужно декодировать payload и проверить claim exp
            return Mono.just(true);
        } catch (Exception e) {
            log.error("JWT validation failed", e);
            return Mono.just(false);
        }
    }
    
    private Mono<Boolean> validateBasic(LinkPolicy.AuthConfig config, String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return Mono.just(false);
        }
        
        // Валидация Basic auth
        // Пока что просто проверяем наличие заголовка
        return Mono.just(true);
    }
    
    private String extractToken(String authHeader) {
        if (authHeader == null) {
            return null;
        }
        
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }
    
    private String extractApiKey(String authHeader, String expectedHeader) {
        // Потом здесь нужно проверять несколько заголовков
        if (authHeader != null && authHeader.startsWith("ApiKey ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private String getClientIp() {
        // Здесь нужно реализовать получение реального IP клиента
        // с учётом заголовков X-Forwarded-For и т.д.
        return "unknown";
    }
}
