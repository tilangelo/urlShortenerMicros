package com.example.shortener_core.application.port.event;

import com.example.shortener_core.application.port.out.CachePort;
import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.domain.model.ShortUrlRedisSerializable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;

/**
 * Сохраняет созданные короткие ссылки в Redis после успешного commit в PostgreSQL.
 * Ошибка кеша логируется и не влияет на уже сохранённую ссылку.
 */
@Component
@Slf4j
public class ShortUrlCacheListener {

    private final CachePort cachePort;

    /**
     * Создаёт listener с доступом к кешу коротких ссылок.
     *
     * @param cachePort порт для работы с кешем коротких ссылок
     */
    public ShortUrlCacheListener(CachePort cachePort) {
        this.cachePort = cachePort;
    }

    /**
     * Рассчитывает оставшийся TTL и сохраняет ссылку в Redis после commit транзакции.
     * Истёкшая ссылка в кеш не записывается.
     *
     * @param event событие с созданной короткой ссылкой
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleShortUrl(ShortUrlCreatedEvent event) {
        ShortUrl shortUrl = event.shortUrl();

        ShortUrlRedisSerializable value = new ShortUrlRedisSerializable(
                shortUrl.getLongUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt()
        );

        try {
            Duration duration = Duration.between(Instant.now(), value.getExpireAt());
            if (duration.isNegative() || duration.isZero()) {
                log.warn("Skipping cache for expired URL, shortcode: {}",
                        shortUrl.getShortCode());
                return;
            }

            boolean saved = cachePort.save(shortUrl.getShortCode(), value, duration);

            if (!saved) {
                log.warn("URL cache entry already exists for shortcode: {}",
                        shortUrl.getShortCode());
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to cache created URL for shortcode: {}",
                    shortUrl.getShortCode(),
                    exception
            );
        }


    }
}
