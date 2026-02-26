package com.example.shortener_core.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShortenRequest {
    @NotBlank(message = "URL is required")
    @Size(max = 2048, message = "URL is too long")
    private String longUrl;

    @NotBlank(message = "Time of exp is required")
    private Long ttl;
}
