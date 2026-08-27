package com.example.shortener_core.application.service;

import com.example.shortener_core.application.port.event.LinkPolicyCreatedEvent;
import com.example.shortener_core.application.port.event.LinkPolicyDeletedEvent;
import com.example.shortener_core.application.port.event.LinkPolicyUpdatedEvent;
import com.example.shortener_core.application.port.in.LinkPolicyManagementUseCase;
import com.example.shortener_core.application.port.out.IdGenerator;
import com.example.shortener_core.application.port.out.LinkPolicyCachePort;
import com.example.shortener_core.application.port.out.LinkPolicyRepositoryPort;
import com.example.shortener_core.application.port.out.UrlRepositoryPort;
import com.example.shortener_core.common.exception.NotSupportedAuthException;
import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.domain.model.LinkPolicyRedis;
import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.domain.valueobject.ShortCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Управляет политиками доступа к коротким ссылкам в PostgreSQL.
 * После commit публикует события для обновления Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinkPolicyService implements LinkPolicyManagementUseCase {
    
    private final LinkPolicyRepositoryPort repository;
    private final LinkPolicyCachePort cache;
    private final IdGenerator idGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final UrlRepositoryPort urlRepository;

    /**
     * Проверяет ссылку и параметры политики, сохраняет её и публикует событие для Redis.
     *
     * @param linkId идентификатор короткой ссылки
     * @param shortcode shortcode этой же ссылки
     * @param allowedIps список разрешённых IP
     * @param allowedTimeStart начало окна доступа, может отсутствовать
     * @param allowedTimeEnd конец окна доступа
     * @param authType тип аутентификации
     * @return сохранённая политика
     */
    @Override
    @Transactional
    public LinkPolicy createPolicy(Long linkId, String shortcode,
                                  List<String> allowedIps,
                                  Instant allowedTimeStart,
                                  Instant allowedTimeEnd,
                                  LinkPolicy.AuthType authType) {

        validateAuthType(authType);
        LinkPolicy.validateTimeWindow(allowedTimeStart, allowedTimeEnd);
        ShortUrl shortUrl = requireMatchingShortUrl(linkId, shortcode);
        
        if (repository.existsByShortcode(shortcode)) {
            throw new IllegalArgumentException("Policy already exists for shortcode: " + shortcode);
        }
        
        if (repository.existsByLinkId(linkId)) {
            throw new IllegalArgumentException("Policy already exists for linkId: " + linkId);
        }
        
        Long id = idGenerator.nextId();
        ShortCode shortCode = ShortCode.of(shortcode);
        
        LinkPolicy linkPolicy = LinkPolicy.create(
            id, linkId, shortCode, allowedIps,
            allowedTimeStart, allowedTimeEnd, authType
        );
        
        LinkPolicy saved = repository.save(linkPolicy);

        eventPublisher.publishEvent(new LinkPolicyCreatedEvent(saved));
        
        log.info("Created link policy for shortcode: {}, linkId: {}", shortcode, linkId);
        return saved;
    }

    /**
     * Находит политику по shortcode.
     *
     * @param shortcode код короткой ссылки
     * @return политика или пустой результат
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<LinkPolicy> getPolicyByShortcode(String shortcode) {
        return repository.findByShortcode(shortcode);
    }

    /**
     * Находит политику по идентификатору ссылки.
     *
     * @param linkId идентификатор короткой ссылки
     * @return политика или пустой результат
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<LinkPolicy> getPolicyByLinkId(Long linkId) {
        return repository.findByLinkId(linkId);
    }

    /**
     * Проверяет и сохраняет новую версию политики, затем публикует событие для Redis.
     *
     * @param linkPolicy обновлённая политика
     * @return сохранённая версия политики
     */
    @Override
    @Transactional
    public LinkPolicy updatePolicy(LinkPolicy linkPolicy) {

        if(linkPolicy == null){
            throw new IllegalArgumentException("linkPolicy cannot be null");
        }

        Long linkId = linkPolicy.getLinkId();
        String shortcode = linkPolicy.getShortcodeValue();

        validateAuthType(linkPolicy.getAuthType());
        LinkPolicy.validateTimeWindow(
                linkPolicy.getAllowedTimeStart(),
                linkPolicy.getAllowedTimeEnd()
        );
        ShortUrl shortUrl = requireMatchingShortUrl(linkId, shortcode);

        LinkPolicy updated = repository.save(linkPolicy);
        
        eventPublisher.publishEvent(new LinkPolicyUpdatedEvent(updated));
        
        log.info("Updated link policy for shortcode: {}", linkPolicy.getShortcodeValue());
        return updated;
    }

    /**
     * Удаляет политику по shortcode и публикует событие для очистки Redis.
     *
     * @param shortcode код ссылки, для которой удаляется политика
     */
    @Override
    @Transactional
    public void deletePolicy(String shortcode) {

        if(shortcode == null || shortcode.isEmpty()){
            throw new IllegalArgumentException("shortcode cannot be null or empty");
        }

        repository.deleteByShortcode(shortcode);
        eventPublisher.publishEvent(new LinkPolicyDeletedEvent(shortcode));
        
        log.info("Deleted link policy for shortcode: {}", shortcode);
    }

    /**
     * Проверяет наличие политики для shortcode.
     *
     * @param shortcode код короткой ссылки
     * @return {@code true}, если политика существует
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasPolicy(String shortcode) {
        return repository.existsByShortcode(shortcode);
    }


    /**
     * Загружает политику из PostgreSQL для fallback-запроса gateway и восстанавливает кеш.
     *
     * @param shortcode код короткой ссылки
     * @return политика в формате Redis или пустой результат
     */
    @Transactional(readOnly = true)
    public Optional<LinkPolicyRedis> getPolicyFromDb(String shortcode) {
        // проверка в БД, если запись есть - сохраняет в redis и возвращает значение
        Optional<LinkPolicy> fromDb = repository.findByShortcode(shortcode);
        if (fromDb.isPresent()) {
            LinkPolicyRedis redisPolicy = LinkPolicyRedis.fromDomain(fromDb.get());
            // обновление кеша

            Duration duration = Duration.between(Instant.now(), redisPolicy.getTime_end());

            cache.savePolicy(shortcode, redisPolicy, duration);
            return Optional.of(redisPolicy);
        }
        
        return Optional.empty();
    }



    /**
     * Проверяет, что тип аутентификации задан и уже поддерживается приложением.
     *
     * @param authType тип аутентификации для проверки
     */
    private void validateAuthType(LinkPolicy.AuthType authType) {
        if(authType == null){
            throw new IllegalArgumentException("authType cannot be null");
        }

        if(authType == LinkPolicy.AuthType.BASIC){
            throw new NotSupportedAuthException("Basic auth type is not supported yet");
        }
    }


    /**
     * Проверяет существование ссылки и соответствие пары linkId/shortcode.
     *
     * @param linkId идентификатор ссылки
     * @param shortcode shortcode этой же ссылки
     * @return найденная короткая ссылка
     */
    private ShortUrl requireMatchingShortUrl(Long linkId, String shortcode) {

        if (linkId == null) {
            throw new IllegalArgumentException("linkId cannot be null");
        }

        if (shortcode == null || shortcode.isBlank()) {
            throw new IllegalArgumentException("shortcode cannot be blank");
        }


        ShortUrl shortUrl = urlRepository.findByShortCode(shortcode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Link not found for shortcode: " + shortcode
                        )
                );

        if (!shortUrl.getId().equals(linkId)) {
            throw new IllegalArgumentException(
                    "linkId does not belong to shortcode: " + shortcode
            );
        }

        return shortUrl;
    }

}
