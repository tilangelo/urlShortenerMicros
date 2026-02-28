package com.example.shortener_core.api.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class RedirectResponse {
    private String shortCode;
    private String longUrl;
    private long ttl;

    public RedirectResponse(String shortCode, String longUrl, long ttl) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.ttl = ttl;
    }
}
