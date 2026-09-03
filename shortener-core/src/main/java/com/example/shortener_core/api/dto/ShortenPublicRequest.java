package com.example.shortener_core.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ShortenPublicRequest {
    @NotBlank(message = "URL обязателен")
    @Size(max = 2048, message = "URL слишком длинный")
    private String longUrl;

    @NotNull
    @Future
    private Instant expiration;
}
