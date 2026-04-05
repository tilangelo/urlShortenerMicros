package com.example.shortener_core.application.port.in;

import com.example.shortener_core.domain.model.LinkPolicy;

import java.util.Optional;

public interface LinkPolicyManagementUseCase {
    
    LinkPolicy createPolicy(Long linkId, String shortcode, 
                           java.util.List<String> allowedIps,
                           java.time.Instant allowedTimeStart,
                           java.time.Instant allowedTimeEnd,
                           LinkPolicy.AuthType authType,
                           String authConfig);
    
    Optional<LinkPolicy> getPolicyByShortcode(String shortcode);
    
    Optional<LinkPolicy> getPolicyByLinkId(Long linkId);
    
    LinkPolicy updatePolicy(LinkPolicy linkPolicy);
    
    void deletePolicy(String shortcode);
    
    boolean hasPolicy(String shortcode);
}
