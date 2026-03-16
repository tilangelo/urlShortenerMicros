package com.shortenerSoft.shortener_redirect.api.controller;


import com.shortenerSoft.shortener_redirect.application.exception.LinkDoesNotExistException;
import com.shortenerSoft.shortener_redirect.application.exception.LinkExpiredException;
import com.shortenerSoft.shortener_redirect.application.port.CacheCheckoutUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/redirect-api/")
public class RedirectController {
    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);
    private final CacheCheckoutUseCase cacheCheckoutUseCase;

    public RedirectController(CacheCheckoutUseCase cacheCheckoutUseCase) {
        this.cacheCheckoutUseCase = cacheCheckoutUseCase;
    }

    @GetMapping("{shortCode}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String shortCode) {

        return cacheCheckoutUseCase.checkout(shortCode)
                // возвращает redirect на ресурс
                .flatMap(longUrl -> {
                    // API Gateway будет использовать этот заголовок для выполнения редиректа
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.FOUND)
                            .header(HttpHeaders.LOCATION, longUrl)
                            .header("X-URL-FOUND", "true")
                            .<Void>build());
                })

                // логирует если редирект
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is3xxRedirection()) {
                        log.info("Redirected shortCode: {} to URL", shortCode);
                    }
                })

                // пропагирую ошибку выше до узла гейтвей
                .onErrorResume(ex -> {

                        if (ex instanceof LinkExpiredException) {
                            log.info("Пользователь уведомлен о том, что эта ссылка устарела");
                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.GONE)
                                    .build());
                        } else if(ex instanceof LinkDoesNotExistException) {
                            log.info("Пользователь уведомлен о том, что ссылка не найдена");
                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.NOT_FOUND)
                                    .build());
                        }else {
                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .build());
                        }
                });
    }

}
