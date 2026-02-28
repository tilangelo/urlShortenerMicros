package com.example.shortener_core.domain.model;

import com.example.shortener_core.domain.valueobject.LongUrl;
import com.example.shortener_core.domain.valueobject.ShortCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

public class ShortUrl {
    // Getters
    @Getter
    private final Long id;
    private final ShortCode shortCode;
    private final LongUrl longUrl;
    @Getter
    private final Instant createdAt;
    @Getter
    private Instant expiresAt;
    @Setter
    private Long clickCount;

    public ShortUrl(Long id, ShortCode shortCode, LongUrl longUrl,
                    Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = 0L;
    }


    // Factory method для создания новой short URL
    public static ShortUrl create(Long id, ShortCode shortCode,
                                  LongUrl longUrl, Long ttl) {
        return new ShortUrl(
                id,
                shortCode, // Генерируем short code из ID
                longUrl,
                Instant.now(),
                Instant.now().plus(Duration.ofMillis(ttl))
        );
    }


    // Методы для редиректа
    public void registerClick() {
        this.clickCount++;
    }

    public String getRedirectUrl() {
        return longUrl.getValue();
    }
    //


    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public String getShortCode() { return shortCode.getValue(); }
    public String getLongUrl() { return longUrl.getValue(); }
    public long getClickCount() { return clickCount; }
}
