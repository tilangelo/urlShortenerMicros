package com.shortenerSoft.shortener_redirect.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ShortenResponse {

    private String shortCode;
    private String longUrl;
    private long ttl;

}
