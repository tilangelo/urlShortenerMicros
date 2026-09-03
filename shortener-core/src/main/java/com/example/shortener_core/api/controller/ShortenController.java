package com.example.shortener_core.api.controller;

import com.example.shortener_core.api.dto.ShortUrlResponse;
import com.example.shortener_core.api.dto.ShortenProtectedRequest;
import com.example.shortener_core.api.dto.ShortenPublicRequest;
import com.example.shortener_core.application.port.in.CreateShortUrlUseCase;
import com.example.shortener_core.application.port.in.CreateProtectedShortUrlUseCase;
import com.example.shortener_core.application.port.in.ProtectedShortUrlResult;
import com.example.shortener_core.domain.model.ShortUrl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;


@RestController
@RequestMapping("/core-api/")
public class ShortenController {

    private final CreateShortUrlUseCase createShortUrlUseCase;
    private final CreateProtectedShortUrlUseCase createProtectedShortUrlUseCase;
    private final String baseUrl;

    public ShortenController(CreateShortUrlUseCase createShortUrlUseCase,
                             CreateProtectedShortUrlUseCase createProtectedShortUrlUseCase,
                             @Value("${base.url:http://localhost:8082}") String baseUrl) {
        this.createShortUrlUseCase = createShortUrlUseCase;
        this.createProtectedShortUrlUseCase = createProtectedShortUrlUseCase;
        this.baseUrl = baseUrl;
    }

    @PostMapping("/shorten/public")
    public ResponseEntity<ShortUrlResponse> shorten(@Valid @RequestBody ShortenPublicRequest request) {
        ShortUrl shortUrl = createShortUrlUseCase.createShortUrl(request.getLongUrl(), request.getExpiration());

        String fullUrl = baseUrl + "/" + shortUrl.getShortCode();

        ShortUrlResponse response = ShortUrlResponse.builder()
                .id(shortUrl.getId())
                .shortcode(shortUrl.getShortCode())
                .shortUrl(fullUrl)
                .createdAt(shortUrl.getCreatedAt())
                .expiresAt(shortUrl.getExpiresAt())
                .build();

        URI location = URI.create(fullUrl);

        return ResponseEntity.created(location).body(response);
    }


    @PostMapping("/shorten/protected")
    public ResponseEntity<ShortUrlResponse> shortenProtected(
            @Valid @RequestBody ShortenProtectedRequest request) {

        ProtectedShortUrlResult result = createProtectedShortUrlUseCase.createProtectedShortUrl(
                request.getLongUrl(),
                request.getAllowedTimeEnd(),
                request.getAllowedIps(),
                request.getAllowedTimeStart(),
                request.getAuthType()
        );

        String fullUrl = baseUrl + "/" + result.shortUrl().getShortCode();

        ShortUrlResponse response = ShortUrlResponse.builder()
                .id(result.shortUrl().getId())
                .shortcode(result.shortUrl().getShortCode())
                .shortUrl(fullUrl)
                .createdAt(result.shortUrl().getCreatedAt())
                .expiresAt(result.shortUrl().getExpiresAt())
                .build();

        URI location = URI.create(fullUrl);

        return ResponseEntity.created(location).body(response);
    }

}
