package com.example.shortener_core.application.service;

import com.example.shortener_core.application.port.event.ShortUrlCreatedEvent;
import com.example.shortener_core.application.port.in.CreateShortUrlUseCase;
import com.example.shortener_core.application.port.out.IdGenerator;
import com.example.shortener_core.application.port.out.UrlRepositoryPort;
import com.example.shortener_core.common.exception.ValidationException;
import com.example.shortener_core.common.util.Base62Encoder;
import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.domain.valueobject.LongUrl;
import com.example.shortener_core.domain.valueobject.ShortCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Создаёт короткие ссылки и сохраняет их в PostgreSQL.
 * Запись в Redis запускается событием только после успешного commit.
 */
@Service
@Slf4j
public class ShortenUrlService implements CreateShortUrlUseCase {

    private final IdGenerator idGenerator;
    private final UrlRepositoryPort urlRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Создаёт сервис с генератором id, хранилищем ссылок и publisher-ом событий.
     *
     * @param idGenerator генератор уникальных идентификаторов
     * @param urlRepository хранилище коротких ссылок
     * @param eventPublisher публикация события для заполнения кеша
     */
    public ShortenUrlService(IdGenerator idGenerator,
                             UrlRepositoryPort urlRepository,
                             ApplicationEventPublisher eventPublisher) {

        this.idGenerator = idGenerator;
        this.urlRepository = urlRepository;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Проверяет URL, создаёт shortcode, сохраняет ссылку и публикует событие для Redis.
     *
     * @param longUrl исходный URL
     * @param expiration момент окончания действия ссылки
     * @return сохранённая короткая ссылка
     */
    @Override
    @Transactional
    public ShortUrl createShortUrl(String longUrl, Instant expiration) {
        log.info("Создание короткой ссылки для {}", longUrl);

        validateLongUrl(longUrl);

        // Генерация id с помощью абстракции(её реализует snowflacke класс)
        Long genId = idGenerator.nextId();
        log.debug("Сгенерирован id: {}", genId);

        ShortUrl shortUrl = createShortCodeAndUrl(genId, longUrl, expiration);

        if (urlRepository.existsByShortCode(shortUrl.getShortCode())) {
            log.error("Обнаружена коллизия шорткода");
            throw new ValidationException("Этот ShortCode уже существует " + shortUrl.getShortCode());
        }


        // Сохранение в pg и Redis
        log.debug("Сохранение в базу данных");
        ShortUrl saved = urlRepository.save(shortUrl);

        eventPublisher.publishEvent(new ShortUrlCreatedEvent(saved));

        log.info("короткая ссылка успешно сохранена в бд");

        // Сохранение в кеш реализовано в ShortUrlCacheListener, сработает после commit транзакции.

        return saved;
    }


    /**
     * Кодирует id в Base62 и собирает доменную модель короткой ссылки.
     *
     * @param id идентификатор ссылки
     * @param longUrl исходный URL
     * @param expiration момент окончания действия ссылки
     * @return новая короткая ссылка
     */
    private ShortUrl createShortCodeAndUrl(Long id, String longUrl, Instant expiration) {
        log.debug("Создание шорткода...");
        // Создание shortCode(Часть shortUrl) с помощью Base62(id -> цифроБуквенный код)
        ShortCode shortCode = ShortCode.of(Base62Encoder.encode(id));

        log.debug("Новый шорткод: {}", shortCode);

        return ShortUrl.create(
                id,
                shortCode,
                LongUrl.of(longUrl),
                expiration
        );
    }

    /**
     * Проверяет, что URL заполнен и не превышает допустимую длину.
     *
     * @param url URL для проверки
     */
    private void validateLongUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            log.error("Пустой URL передан");
            throw new ValidationException("URL cannot be empty");
        }
        if (url.length() > 2048) {
            log.error("URL превышает максимальную длину");
            throw new ValidationException("URL is too long");
        }
    }

}
