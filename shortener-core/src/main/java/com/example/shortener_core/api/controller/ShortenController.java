package com.example.shortener_core.api.controller;

import com.example.shortener_core.api.dto.RedirectResponse;
import com.example.shortener_core.api.dto.ShortenRequest;
import com.example.shortener_core.api.dto.ShortenResponse;
import com.example.shortener_core.application.port.in.CreateShortUrlUseCase;
import com.example.shortener_core.application.port.in.RedirectUseCase;
import com.example.shortener_core.common.exception.LinkExpiredException;
import com.example.shortener_core.common.exception.NotFoundException;
import com.example.shortener_core.domain.model.ShortUrl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/core-api/v1/")
public class ShortenController {

    private final CreateShortUrlUseCase createShortUrlUseCase;
    private final String baseUrl;
    private final RedirectUseCase redirectUseCase;

    public ShortenController(CreateShortUrlUseCase createShortUrlUseCase,
                             @Value("${app.base-url:http://localhost:8080}") String baseUrl,
                             RedirectUseCase redirectUseCase) {
        this.createShortUrlUseCase = createShortUrlUseCase;
        this.baseUrl = baseUrl;
        this.redirectUseCase = redirectUseCase;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortUrl shortUrl = createShortUrlUseCase.createShortUrl(request.getLongUrl(), request.getTtl());

        String fullUrl = baseUrl + "/" + shortUrl.getShortCode();

        return ResponseEntity.ok(new ShortenResponse(fullUrl));
    }

    @GetMapping("/getExcUrl/{shortCode}")
    public ResponseEntity<RedirectResponse> getExcUrl(@PathVariable String shortCode) throws Exception {

        System.out.println("Пришёл запрос от redirect сервиса...");

        try {
            RedirectResponse response = redirectUseCase.redirect(shortCode);

            System.out.println("Отправялется ответ...");

            System.out.println("LongUrl от ответа: " + response.getLongUrl() +
                    " || ShortCode от ответа: " + response.getShortCode() +
                    " || ttl от ответа: " + response.getTtl());

            return ResponseEntity.ok(response);
        }catch (LinkExpiredException e){
            System.out.println("Отправляется GONE, Stack trace:  " + Arrays.toString(e.getStackTrace()));
            return ResponseEntity.status(HttpStatus.GONE).build();
        }catch (NotFoundException e) {
            System.out.println("Код не найден");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }catch (Exception e) {
            System.out.println("непредвиденная ошибка возвращается в redirect " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
