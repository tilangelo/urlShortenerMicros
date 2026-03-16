package com.shortenerSoft.shortener_redirect.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortUrlRedisSerializable implements Serializable {
    private String longUrl;
    private Instant createdAt;
    private Instant expireAt;
}
