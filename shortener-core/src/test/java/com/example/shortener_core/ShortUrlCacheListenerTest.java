package com.example.shortener_core;

import com.example.shortener_core.application.port.event.ShortUrlCacheListener;
import com.example.shortener_core.application.port.event.ShortUrlCreatedEvent;
import com.example.shortener_core.application.port.out.CachePort;
import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.domain.model.ShortUrlRedisSerializable;
import com.example.shortener_core.domain.valueobject.LongUrl;
import com.example.shortener_core.domain.valueobject.ShortCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShortUrlCacheListenerTest {

    @Mock
    private CachePort cachePort;

    @InjectMocks
    private ShortUrlCacheListener listener;


    @Test
    void handleShortUrl_whenUrlIsActive_savesValueWithRemainingTtl(){
        Instant expiresAt = Instant.now().plus(Duration.ofHours(1));

        ShortUrl shortUrl = ShortUrl.create(
                42L,
                ShortCode.of("abc123"),
                LongUrl.of("https://example.com"),
                expiresAt
        );

        when(cachePort.save(
                anyString(),
                any(ShortUrlRedisSerializable.class),
                any(Duration.class)
        )).thenReturn(true);


        listener.handleShortUrl(new ShortUrlCreatedEvent(shortUrl));

        ArgumentCaptor<ShortUrlRedisSerializable> shortUrlArgumentCaptor =
                ArgumentCaptor.forClass(ShortUrlRedisSerializable.class);

        ArgumentCaptor<Duration> durationCaptor =
                ArgumentCaptor.forClass(Duration.class);

        verify(cachePort).save(
                eq("abc123"),
                shortUrlArgumentCaptor.capture(),
                durationCaptor.capture()
        );

        assertEquals(shortUrl.getLongUrl(), shortUrlArgumentCaptor.getValue().getLongUrl());
        assertEquals(expiresAt, shortUrlArgumentCaptor.getValue().getExpireAt());
        Duration ttl = durationCaptor.getValue();
        assertTrue(ttl.compareTo(Duration.ZERO) > 0);
        assertTrue(ttl.compareTo(Duration.ofHours(1)) <= 0);
    }


    @Test
    void handleShortUrl_whenUrlIsExpired_doesNotSaveValue(){
        Instant expiresAt = Instant.now().minusSeconds(1);

        ShortUrl shortUrl = ShortUrl.create(
                42L,
                ShortCode.of("abc123"),
                LongUrl.of("https://example.com"),
                expiresAt
        );

        listener.handleShortUrl(new ShortUrlCreatedEvent(shortUrl));

        verifyNoInteractions(cachePort);

    }

}
