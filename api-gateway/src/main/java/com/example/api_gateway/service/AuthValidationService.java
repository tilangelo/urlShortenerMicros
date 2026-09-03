package com.example.api_gateway.service;

import com.example.api_gateway.model.exception.UnsupportedAuthenticationTypeException;
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

        if (authType == null ) {
            return Mono.just(true); // Аутентификация не требуется
        }

        String authHeader = request.getHeaders().getFirst("Authorization");

        switch (authType) {
            case "corporate_sso":
                return validateCorporateSso(authHeader, clientIp);
            case "api_key":
                return validateApiKey(authHeader, clientIp);
            case "basic":
                log.warn("Basic auth not supported");
                return Mono.error(
                        new UnsupportedAuthenticationTypeException(
                                "Basic authentication is not supported"
                        )
                );
            default:
                log.warn("Unsupported authentication type: {}", authType);
                return Mono.error(
                        new UnsupportedAuthenticationTypeException(
                                "Unsupported authentication type: " + authType
                        )
                );
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


    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        if (token.isEmpty()) {
            return null;
        }

        return token;
    }

}
