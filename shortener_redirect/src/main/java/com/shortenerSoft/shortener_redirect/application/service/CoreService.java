package com.shortenerSoft.shortener_redirect.application.service;

import com.shortenerSoft.shortener_redirect.api.dto.ShortenResponse;
import com.shortenerSoft.shortener_redirect.application.exception.LinkExpiredException;
import com.shortenerSoft.shortener_redirect.application.port.CorePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
public class CoreService implements CorePort {

    private final WebClient webClient;

    @Autowired
    public CoreService(@Qualifier("coreWebClient") WebClient webClient) {
        this.webClient = webClient;
    }


    @Override
    public Mono<ShortenResponse> findLongUrlByShortCode(String shortCode) {
        System.out.println("Вызвылся метод fingLongUrlByShortCode");
        return webClient.get()
                .uri("http://localhost:8080/core-api/v1/getExcUrl/{shortCode}", shortCode)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    // Логируем статус ошибки прямо здесь
                    System.out.println("CORE вернул ошибку: " + response.statusCode());
                    return response.createException();
                })
                .bodyToMono(ShortenResponse.class)
                .log("WEB-CLIENT")
                .onErrorResume(ex -> {
                    if (ex instanceof WebClientResponseException.Gone) {
                        log.warn("Код {} просрочен и удален в Core", shortCode);
                        return Mono.error(new LinkExpiredException("URL has expired"));

                    } else if (ex instanceof WebClientResponseException.NotFound) {
                        log.warn("Код {} не найден в Core", shortCode);
                        return Mono.empty();

                    } else {
                        log.error("Непредвиденная ошибка Core: {}", ex.getMessage());
                        return Mono.error(ex); // Пропагируем ошибку дальше
                    }
                });
    }
}
