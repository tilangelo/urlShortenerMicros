package com.example.shortener_core.api.controller;

import com.example.shortener_core.api.dto.CreateLinkPolicyRequest;
import com.example.shortener_core.api.dto.LinkPolicyResponse;
import com.example.shortener_core.application.service.LinkPolicyService;
import com.example.shortener_core.domain.model.LinkPolicy;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core-api/policies")
@RequiredArgsConstructor
@Slf4j
public class LinkPolicyManagementController {
    
    private final LinkPolicyService linkPolicyService;
    
    @PostMapping
    public ResponseEntity<LinkPolicyResponse> createPolicy(@Valid @RequestBody CreateLinkPolicyRequest request) {
        log.info("Создание policy для shortcode: {}, linkId: {}", request.getShortcode(), request.getLinkId());
        
        LinkPolicy created = linkPolicyService.createPolicy(
            request.getLinkId(),
            request.getShortcode(),
            request.getAllowedIps(),
            request.getAllowedTimeStart(),
            request.getAllowedTimeEnd(),
            request.getAuthType()
        );
        
        return ResponseEntity.ok(LinkPolicyResponse.fromRedis(
            com.example.shortener_core.domain.model.LinkPolicyRedis.fromDomain(created)
        ));
    }
    
    @GetMapping("/{shortcode}")
    public ResponseEntity<LinkPolicyResponse> getPolicy(@PathVariable String shortcode) {
        return linkPolicyService.getPolicyByShortcode(shortcode)
                .map(policy -> LinkPolicyResponse.fromRedis(
                    com.example.shortener_core.domain.model.LinkPolicyRedis.fromDomain(policy)
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{shortcode}")
    public ResponseEntity<Void> deletePolicy(@PathVariable String shortcode) {
        linkPolicyService.deletePolicy(shortcode);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{shortcode}/exists")
    public ResponseEntity<Boolean> checkExists(@PathVariable String shortcode) {
        boolean exists = linkPolicyService.hasPolicy(shortcode);
        return ResponseEntity.ok(exists);
    }
}
