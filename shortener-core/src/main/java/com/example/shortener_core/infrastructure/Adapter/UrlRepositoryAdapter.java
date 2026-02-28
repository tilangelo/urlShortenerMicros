package com.example.shortener_core.infrastructure.Adapter;

import com.example.shortener_core.application.port.out.UrlRepositoryPort;
import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.infrastructure.persistence.entity.UrlEntity;
import com.example.shortener_core.infrastructure.persistence.mapper.UrlMapper;
import com.example.shortener_core.infrastructure.persistence.repository.JpaUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class UrlRepositoryAdapter implements UrlRepositoryPort {
    private final JpaUrlRepository jpaUrlRepository;
    private final UrlMapper urlMapper;
    private static final Logger log = LoggerFactory.getLogger(UrlRepositoryAdapter.class);

    public UrlRepositoryAdapter(JpaUrlRepository jpaUrlRepository, UrlMapper urlMapper) {
        this.jpaUrlRepository = jpaUrlRepository;
        this.urlMapper = urlMapper;
    }


    @Override
    @Transactional
    public ShortUrl save(ShortUrl shortUrl) {
        // Domain model -> сущность
        UrlEntity entity = urlMapper.toEntity(shortUrl);

        // Сохраняем через JPA репозиторий
        UrlEntity savedEntity = jpaUrlRepository.save(entity);

        // Конвертируем обратно Сущность -> Domain Model
        return urlMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShortUrl> findByShortCode(String shortCode) {
        // Ищем сущность по shortCode
        Optional<UrlEntity> entity = jpaUrlRepository.findByShortCode(shortCode);

        // Обратно в Domain model если нашли
        return entity.map(urlMapper::toDomain);
    }

    @Override
    public boolean existsByShortCode(String shortCode) {
        return jpaUrlRepository.existsByShortCode(shortCode);
    }

    @Override
    public void incrementClickCount(String shortCode) {
        jpaUrlRepository.incrementClickCount(shortCode);
    }

    @Override
    public UrlEntity findByLongUrl(String longUrl) {
        return jpaUrlRepository.findByLongUrl(longUrl);
    }

    @Override
    @Transactional // ДОБАВЛЕНО: Транзакция для операций записи
    public boolean deleteByShortCode(String shortCode) {
        // ИЗМЕНЕНО: Сравниваем с > 0, так как метод возвращает int
        return jpaUrlRepository.deleteByShortCode(shortCode) > 0;
    }

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // Каждые 24 часа
    @Transactional
    public void cleanExpiredUrls() {
        int deleted = jpaUrlRepository.deleteExpiredUrls();
        log.info("Удалено {} устаревших URL", deleted);
    }
}
