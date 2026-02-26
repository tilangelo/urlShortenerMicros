package com.shortenerSoft.shortener_redirect.api.dto;

import lombok.Data;

@Data
public class ShortenResponse {

    private String longUrl;
    private String shortCode;
    private long ttl;

}
