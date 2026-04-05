package com.example.shortener_core.application.port.out;

import com.example.shortener_core.domain.model.LinkPolicy;

import java.util.Optional;

public interface LinkPolicyRepositoryPort {
    
    LinkPolicy save(LinkPolicy linkPolicy);
    
    Optional<LinkPolicy> findByShortcode(String shortcode);
    
    Optional<LinkPolicy> findByLinkId(Long linkId);
    
    void deleteByShortcode(String shortcode);
    
    void deleteByLinkId(Long linkId);
    
    boolean existsByShortcode(String shortcode);
    
    boolean existsByLinkId(Long linkId);
}
