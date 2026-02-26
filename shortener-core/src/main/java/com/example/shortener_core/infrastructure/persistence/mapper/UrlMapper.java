package com.example.shortener_core.infrastructure.persistence.mapper;

import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.domain.valueobject.LongUrl;
import com.example.shortener_core.domain.valueobject.ShortCode;
import com.example.shortener_core.infrastructure.persistence.entity.UrlEntity;
import org.springframework.stereotype.Component;

@Component
public class UrlMapper {
    public UrlEntity toEntity(ShortUrl shortUrl) {
        UrlEntity entity = new UrlEntity(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                shortUrl.getLongUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt()
        );
        entity.setClickCount(shortUrl.getClickCount());
        return entity;
    }

    public ShortUrl toDomain(UrlEntity entity) {
        ShortUrl url = new ShortUrl(
                entity.getId(),
                ShortCode.of(entity.getShortCode()),
                LongUrl.of(entity.getLongUrl()),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
        url.setClickCount(entity.getClickCount()); // нужен setter или factory
        return url;
    }
}
