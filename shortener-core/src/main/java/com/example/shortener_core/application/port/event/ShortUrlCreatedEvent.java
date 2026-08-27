package com.example.shortener_core.application.port.event;

import com.example.shortener_core.domain.model.ShortUrl;

/**
 * Передаёт созданную короткую ссылку listener-у для сохранения в Redis после commit транзакции.
 *
 * @param shortUrl сохранённая короткая ссылка
 */
public record ShortUrlCreatedEvent(ShortUrl shortUrl) {
}
