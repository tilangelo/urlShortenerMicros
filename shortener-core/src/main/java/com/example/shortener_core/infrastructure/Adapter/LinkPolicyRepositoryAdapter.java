package com.example.shortener_core.infrastructure.Adapter;

import com.example.shortener_core.application.port.out.LinkPolicyRepositoryPort;
import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.infrastructure.persistence.entity.LinkPolicyEntity;
import com.example.shortener_core.infrastructure.persistence.mapper.LinkPolicyMapper;
import com.example.shortener_core.infrastructure.persistence.repository.JpaLinkPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LinkPolicyRepositoryAdapter implements LinkPolicyRepositoryPort {
    
    private final JpaLinkPolicyRepository jpaRepository;
    private final LinkPolicyMapper mapper;
    
    @Override
    public LinkPolicy save(LinkPolicy linkPolicy) {
        LinkPolicyEntity entity = mapper.toEntity(linkPolicy);
        LinkPolicyEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<LinkPolicy> findByShortcode(String shortcode) {
        return jpaRepository.findByShortcode(shortcode)
                .map(mapper::toDomain);
    }
    
    @Override
    public Optional<LinkPolicy> findByLinkId(Long linkId) {
        return jpaRepository.findByLinkId(linkId)
                .map(mapper::toDomain);
    }
    
    @Override
    public void deleteByShortcode(String shortcode) {
        jpaRepository.findByShortcode(shortcode)
                .ifPresent(jpaRepository::delete);
    }
    
    @Override
    public void deleteByLinkId(Long linkId) {
        jpaRepository.findByLinkId(linkId)
                .ifPresent(jpaRepository::delete);
    }
    
    @Override
    public boolean existsByShortcode(String shortcode) {
        return jpaRepository.existsByShortcode(shortcode);
    }
    
    @Override
    public boolean existsByLinkId(Long linkId) {
        return jpaRepository.existsByLinkId(linkId);
    }
}
