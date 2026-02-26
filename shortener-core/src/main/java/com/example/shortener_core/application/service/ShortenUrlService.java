package com.example.shortener_core.application.service;

import com.example.shortener_core.application.port.in.CreateShortUrlUseCase;
import com.example.shortener_core.application.port.out.CachePort;
import com.example.shortener_core.application.port.out.IdGenerator;
import com.example.shortener_core.application.port.out.UrlRepositoryPort;
import com.example.shortener_core.common.exception.ValidationException;
import com.example.shortener_core.common.util.Base62Encoder;
import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.domain.valueobject.LongUrl;
import com.example.shortener_core.domain.valueobject.ShortCode;
import com.example.shortener_core.infrastructure.persistence.entity.UrlEntity;
import com.example.shortener_core.infrastructure.persistence.mapper.UrlMapper;
import com.example.shortener_core.infrastructure.persistence.repository.JpaUrlRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class ShortenUrlService implements CreateShortUrlUseCase {

    private final IdGenerator idGenerator;
    private final UrlRepositoryPort urlRepository;
    private final CachePort cachePort;
    private final UrlMapper urlMapper;

    public ShortenUrlService(IdGenerator idGenerator,
                             UrlRepositoryPort urlRepository,
                             CachePort cachePort, UrlMapper urlMapper) {

        this.idGenerator = idGenerator;
        this.urlRepository = urlRepository;
        this.cachePort = cachePort;
        this.urlMapper = urlMapper;
    }




    @Override
    public ShortUrl createShortUrl(String longUrl, Long ttl) {
            validateLongUrl(longUrl);

            // Генерация id с помощью абстракции(её реализует snowflacke класс)
            Long genId = idGenerator.nextId();
            ShortUrl shortUrl = createShortCodeAndUrl(genId, longUrl, ttl);

            if(urlRepository.existsByShortCode(shortUrl.getShortCode())) {
                throw new ValidationException("Этот ShortCode уже существует " + shortUrl.getShortCode());
            }


            // Сохранение в БД и КЕШ
            urlRepository.save(shortUrl);

            // Если ttl больше 30 минут, то сохраняю в REDIS с дефолтным ttl - 30 минут
            if(ttl > 1800000){
                cachePort.saveWithDefaultTtl(shortUrl.getShortCode(), longUrl);

                // Если меньше или равно 30 минут, то сохраняю в REDIS с этим временем
            }else {
                cachePort.save(shortUrl.getShortCode(), longUrl, ttl);
            }

            return shortUrl;
    }




    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ


    //СОздание короткого URL
    private ShortUrl createShortCodeAndUrl(Long id, String longUrl, Long ttl) {
        // Создание shortCode(Часть shortUrl) с помощью Base62(id -> цифроБуквенный код)
        ShortCode shortCode = ShortCode.of(Base62Encoder.encode(id));

        return ShortUrl.create(
                id,
                shortCode,
                LongUrl.of(longUrl),
                ttl
        );
    }

    //Валидация строки URL на разумные размеры
    private void validateLongUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new ValidationException("URL cannot be empty");
        }
        if (url.length() > 2048) {
            throw new ValidationException("URL is too long");
        }
    }
}
