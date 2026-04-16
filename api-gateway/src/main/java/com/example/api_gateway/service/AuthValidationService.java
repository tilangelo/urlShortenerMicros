package com.example.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthValidationService {

    private final AuthEndpointExchange authEndpointExchange;

    private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration SSO_TIMEOUT = Duration.ofSeconds(5);

    public Mono<Boolean> validateAuth(String authType,
                                      ServerHttpRequest request,
                                      String clientIp) {

        if (authType == null || authType.equals("null") || authType.isEmpty()) {
            return Mono.just(true); // Аутентификация не требуется
        }

        String authHeader = request.getHeaders().getFirst("Authorization");

        switch (authType) {
            case "corporate_sso":
                return validateCorporateSso(authHeader, clientIp);
            case "api_key":
                return validateApiKey(authHeader, clientIp);
            case "basic":
                return validateBasic(authHeader);
            default:
                log.warn("Unknown auth type: {}", authType);
                return Mono.just(false);
        }
    }

    // Валидация токена sso через внешний эндпоинт
    private Mono<Boolean> validateCorporateSso(String authHeader, String clientIp) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            log.warn("Пустой SSO token");
            return Mono.just(false);
        }

        // берется из .env с дефолтным значением
        //TODO: СЕЙЧАС ПРИ FALSE ТОЛЬКО ЛОГ, ДАЛЬШЕ МОЖНО ИНТЕГРИРОВАТЬ С СЕРВИСОМ И ПРИ FALSE РЕДИРЕКТ НА АВТОРИЗАЦИЮ
        return authEndpointExchange.corpSsoExchange(token, clientIp, SSO_TIMEOUT);
    }

    // Валидация API ключа через внешний сервис
    private Mono<Boolean> validateApiKey(String authHeader, String clientIp) {
        String apiKey = extractBearerToken(authHeader);
        if (apiKey == null) {
            log.warn("Empty API key");
            return Mono.just(false);
        }

        // endpoint берется из .env или дефолтное значение
        //TODO: СЕЙЧАС ПРИ FALSE ТОЛЬКО ЛОГ, ДАЛЬШЕ МОЖНО ИНТЕГРИРОВАТЬ С СЕРВИСОМ И ПРИ FALSE РЕДИРЕКТ НА АВТОРИЗАЦИЮ
        return authEndpointExchange.apiKeyExchange(apiKey, clientIp, AUTH_TIMEOUT);
    }


    // Валидация без ключей и авторизации, то есть если политика ограничила только ip и/или время
    private Mono<Boolean> validateBasic(String authHeader) {
        return Mono.just(authHeader != null && authHeader.startsWith("Basic "));
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null) {
            return null;
        }

        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        return null;
    }

}
