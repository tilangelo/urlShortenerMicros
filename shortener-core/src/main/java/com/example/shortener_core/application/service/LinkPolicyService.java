package com.example.shortener_core.application.service;

import com.example.shortener_core.application.port.in.LinkPolicyManagementUseCase;
import com.example.shortener_core.application.port.out.IdGenerator;
import com.example.shortener_core.application.port.out.LinkPolicyCachePort;
import com.example.shortener_core.application.port.out.LinkPolicyRepositoryPort;
import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.domain.model.LinkPolicyRedis;
import com.example.shortener_core.domain.valueobject.ShortCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkPolicyService implements LinkPolicyManagementUseCase {
    
    private final LinkPolicyRepositoryPort repository;
    private final LinkPolicyCachePort cache;
    private final IdGenerator idGenerator;
    
    @Override
    @Transactional
    public LinkPolicy createPolicy(Long linkId, String shortcode, 
                                  List<String> allowedIps,
                                  Instant allowedTimeStart,
                                  Instant allowedTimeEnd,
                                  LinkPolicy.AuthType authType,
                                  String authConfig) {
        
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
            allowedTimeStart, allowedTimeEnd, authType, authConfig
        );
        
        LinkPolicy saved = repository.save(linkPolicy);
        
        // Сохранение в Redis
        LinkPolicyRedis redisPolicy = LinkPolicyRedis.fromDomain(saved);
        cache.savePolicy(shortcode, redisPolicy);
        
        log.info("Created link policy for shortcode: {}, linkId: {}", shortcode, linkId);
        return saved;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<LinkPolicy> getPolicyByShortcode(String shortcode) {
        return repository.findByShortcode(shortcode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<LinkPolicy> getPolicyByLinkId(Long linkId) {
        return repository.findByLinkId(linkId);
    }
    
    @Override
    @Transactional
    public LinkPolicy updatePolicy(LinkPolicy linkPolicy) {
        LinkPolicy updated = repository.save(linkPolicy);
        
        // Обновление Redis
        LinkPolicyRedis redisPolicy = LinkPolicyRedis.fromDomain(updated);
        cache.savePolicy(linkPolicy.getShortcodeValue(), redisPolicy);
        
        log.info("Updated link policy for shortcode: {}", linkPolicy.getShortcodeValue());
        return updated;
    }
    
    @Override
    @Transactional
    public void deletePolicy(String shortcode) {
        repository.deleteByShortcode(shortcode);
        cache.deletePolicy(shortcode);
        
        log.info("Deleted link policy for shortcode: {}", shortcode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasPolicy(String shortcode) {
        return repository.existsByShortcode(shortcode);
    }
    
    // Метод для fallback из gateway если тот не нашёл запись в redis(или не смог прочитать)
    @Transactional(readOnly = true)
    public Optional<LinkPolicyRedis> getPolicyFromDb(String shortcode) {
        // проверка в БД, если запись есть - сохраняет в redis и возвращает значение
        Optional<LinkPolicy> fromDb = repository.findByShortcode(shortcode);
        if (fromDb.isPresent()) {
            LinkPolicyRedis redisPolicy = LinkPolicyRedis.fromDomain(fromDb.get());
            // обновление кеша
            cache.savePolicy(shortcode, redisPolicy);
            return Optional.of(redisPolicy);
        }
        
        return Optional.empty();
    }
}
