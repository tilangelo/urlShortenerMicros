package com.example.shortener_core.infrastructure.persistence.mapper;

import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.domain.valueobject.ShortCode;
import com.example.shortener_core.infrastructure.persistence.entity.LinkPolicyEntity;
import org.springframework.stereotype.Component;

@Component
public class LinkPolicyMapper {
    
    public LinkPolicy toDomain(LinkPolicyEntity entity) {
        return new LinkPolicy(
            entity.getId(),
            entity.getLinkId(),
            ShortCode.of(entity.getShortcode()),
            entity.getAllowedIps(),
            entity.getAllowedTimeStart(),
            entity.getAllowedTimeEnd(),
            entity.getAuthTypeEnum(),
            entity.getAuthConfig(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    public LinkPolicyEntity toEntity(LinkPolicy domain) {
        return new LinkPolicyEntity(
            domain.getId(),
            domain.getLinkId(),
            domain.getShortcodeValue(),
            domain.getAllowedIps(),
            domain.getAllowedTimeStart(),
            domain.getAllowedTimeEnd(),
            domain.getAuthType(),
            domain.getAuthConfig(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }
}
