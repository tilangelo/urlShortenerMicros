package com.shortenerSoft.shortener_redirect.api.controller;


import com.shortenerSoft.shortener_redirect.application.port.CacheCheckoutUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/redirect-api/v1/")
public class RedirectController {
    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);
    private final CacheCheckoutUseCase cacheCheckoutUseCase;

    public RedirectController(CacheCheckoutUseCase cacheCheckoutUseCase) {
        this.cacheCheckoutUseCase = cacheCheckoutUseCase;
    }

    @GetMapping("{shortCode}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String shortCode,
                                               ServerWebExchange exchange) {
        return cacheCheckoutUseCase.checkout(shortCode)
                .flatMap(longUrl -> {
                    // API Gateway будет использовать этот заголовок для выполнения редиректа
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.FOUND)
                            .header(HttpHeaders.LOCATION, longUrl)
                            .header("X-URL-FOUND", "true")
                            .<Void>build()); // Явное указание типа
                })
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .header("X-URL-FOUND", "false")
                        .<Void>build())) // Явное указание типа
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is3xxRedirection()) {
                        log.info("Redirected shortCode: {} to URL", shortCode);
                    } else {
                        log.warn("Short code not found: {}", shortCode);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error processing redirect for shortCode: {}", shortCode, e);
                    // Возвращаем 500 ошибку вместо пропагирования исключения
                    return Mono.just(ResponseEntity
                            .<Void>status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build());
                });
    }
}
