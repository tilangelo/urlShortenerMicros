package com.example.shortener_core.application.port.out;

import com.example.shortener_core.domain.model.LinkPolicyRedis;

import java.util.Optional;

public interface LinkPolicyCachePort {
    
    void savePolicy(String shortcode, LinkPolicyRedis policy);
    
    Optional<LinkPolicyRedis> getPolicy(String shortcode);
    
    void deletePolicy(String shortcode);
    
    boolean existsPolicy(String shortcode);
}
