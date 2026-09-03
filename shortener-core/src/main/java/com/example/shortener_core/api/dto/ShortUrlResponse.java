package com.example.shortener_core.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class ShortUrlResponse {

    private Long id;
    private String shortcode;
    private String shortUrl;
    private Instant createdAt;
    private Instant expiresAt;

}
