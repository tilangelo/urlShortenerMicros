package com.example.shortener_core.application.service;

import com.example.shortener_core.application.port.in.CreateShortUrlUseCase;
import com.example.shortener_core.application.port.out.CachePort;
import com.example.shortener_core.application.port.out.IdGenerator;
import com.example.shortener_core.application.port.out.UrlRepositoryPort;
import com.example.shortener_core.common.exception.ValidationException;
import com.example.shortener_core.common.util.Base62Encoder;
import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.domain.model.ShortUrlRedisSerializable;
import com.example.shortener_core.domain.valueobject.LongUrl;
import com.example.shortener_core.domain.valueobject.ShortCode;
import com.example.shortener_core.infrastructure.persistence.mapper.UrlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShortenUrlService implements CreateShortUrlUseCase {

    private final IdGenerator idGenerator;
    private final UrlRepositoryPort urlRepository;
    private final CachePort cachePort;

    public ShortenUrlService(IdGenerator idGenerator,
                             UrlRepositoryPort urlRepository,
                             CachePort cachePort) {

        this.idGenerator = idGenerator;
        this.urlRepository = urlRepository;
        this.cachePort = cachePort;
    }


    @Override
    public ShortUrl createShortUrl(String longUrl, Long ttl) {
        log.info("Создание короткой ссылки для {}", longUrl);

        validateLongUrl(longUrl);

        // Генерация id с помощью абстракции(её реализует snowflacke класс)
        Long genId = idGenerator.nextId();
        log.debug("Сгенерирован id: {}", genId);

        ShortUrl shortUrl = createShortCodeAndUrl(genId, longUrl, ttl);

        if (urlRepository.existsByShortCode(shortUrl.getShortCode())) {
            log.error("Обнаружена коллизия шорткода");
            throw new ValidationException("Этот ShortCode уже существует " + shortUrl.getShortCode());
        }


        // Сохранение в pg и Redis
        log.debug("Сохранение в базу данных");
        urlRepository.save(shortUrl);

        // Сохранение в Redis в формате: url:SHORTCODE -> {longUrl, expiredAt, createdAt}
        ShortUrlRedisSerializable serializable = new ShortUrlRedisSerializable(
                longUrl,
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt()
        );
        cachePort.save(shortUrl.getShortCode(), serializable);

        log.info("короткая ссылка успешно сохранена");

        return shortUrl;
    }


    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ


    // Создание объекта короткой ссылки
    private ShortUrl createShortCodeAndUrl(Long id, String longUrl, Long ttl) {
        log.debug("Создание шорткода...");
        // Создание shortCode(Часть shortUrl) с помощью Base62(id -> цифроБуквенный код)
        ShortCode shortCode = ShortCode.of(Base62Encoder.encode(id));

        log.debug("Новый шорткод: {}", shortCode);

        return ShortUrl.create(
                id,
                shortCode,
                LongUrl.of(longUrl),
                ttl
        );
    }

    // Валидация URL на корректность и допустимую длину
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
