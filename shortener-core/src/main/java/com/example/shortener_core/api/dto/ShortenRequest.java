package com.example.shortener_core.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShortenRequest {
    @NotBlank(message = "URL обязателен")
    @Size(max = 2048, message = "URL слишком длинный")
    private String longUrl;

    @NotNull
    @Min(1)
    private Long ttl;
}
