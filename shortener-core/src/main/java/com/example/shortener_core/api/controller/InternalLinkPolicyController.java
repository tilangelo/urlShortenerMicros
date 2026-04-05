package com.example.shortener_core.api.controller;

import com.example.shortener_core.api.dto.LinkPolicyResponse;
import com.example.shortener_core.application.service.LinkPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/links")
@RequiredArgsConstructor
@Slf4j
public class InternalLinkPolicyController {
    
    private final LinkPolicyService linkPolicyService;
    
    @GetMapping("/{shortcode}/policy")
    public ResponseEntity<LinkPolicyResponse> getLinkPolicy(@PathVariable String shortcode) {
        log.debug("Getting policy for shortcode: {}", shortcode);
        
        return linkPolicyService.getPolicyFromCacheOrDb(shortcode)
                .map(LinkPolicyResponse::fromRedis)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{shortcode}/policy-exists")
    public ResponseEntity<Boolean> checkPolicyExists(@PathVariable String shortcode) {
        boolean exists = linkPolicyService.hasPolicy(shortcode);
        return ResponseEntity.ok(exists);
    }
}
