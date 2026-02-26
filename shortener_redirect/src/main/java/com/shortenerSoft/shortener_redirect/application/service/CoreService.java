package com.shortenerSoft.shortener_redirect.application.service;

import com.shortenerSoft.shortener_redirect.api.dto.ShortenResponse;
import com.shortenerSoft.shortener_redirect.application.port.CorePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class CoreService implements CorePort {

    private final WebClient webClient;

    @Autowired
    public CoreService(@Qualifier("coreWebClient") WebClient webClient) {
        this.webClient = webClient;
    }


    @Override
    public Mono<ShortenResponse> findLongUrlByShortCode(String shortCode) {
        return webClient.get()
                .uri("/core-api/v1/getExcUrl/{shortCode}", shortCode)
                .retrieve()
                .bodyToMono(ShortenResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class,
                        ex -> Mono.empty()); // Возвращаем пустой Mono если URL не найден
    }
}
