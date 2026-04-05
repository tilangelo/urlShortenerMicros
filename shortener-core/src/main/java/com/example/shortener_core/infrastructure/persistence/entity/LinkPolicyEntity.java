package com.example.shortener_core.infrastructure.persistence.entity;

import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.infrastructure.persistence.converter.ListToJsonConverter;
import com.example.shortener_core.infrastructure.persistence.converter.StringToJsonConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "link_policies", indexes = {
    @Index(name = "idx_link_policies_shortcode", columnList = "shortcode", unique = true),
    @Index(name = "idx_link_policies_link_id", columnList = "link_id"),
    @Index(name = "idx_link_policies_time_window", columnList = "allowed_time_start, allowed_time_end")
})
@Getter
@Setter
public class LinkPolicyEntity {
    
    private static final JsonMapper jsonMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "link_id", nullable = false)
    private Long linkId;
    
    @Column(name = "shortcode", nullable = false, unique = true, length = 10)
    private String shortcode;
    
    @Column(name = "allowed_ips")
@JdbcTypeCode(SqlTypes.JSON)
@Convert(converter = ListToJsonConverter.class)
private List<String> allowedIps;
    
    @Column(name = "allowed_time_start")
    private Instant allowedTimeStart;
    
    @Column(name = "allowed_time_end")
    private Instant allowedTimeEnd;
    
    @Column(name = "auth_type", length = 50)
    private String authType;
    
    @Column(name = "auth_config")
@JdbcTypeCode(SqlTypes.JSON)
@Convert(converter = StringToJsonConverter.class)
private String authConfig;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    public LinkPolicyEntity() {}
    
    public LinkPolicyEntity(Long id, Long linkId, String shortcode,
                           List<String> allowedIps, Instant allowedTimeStart,
                           Instant allowedTimeEnd, LinkPolicy.AuthType authType,
                           String authConfig, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.linkId = linkId;
        this.shortcode = shortcode;
        this.allowedIps = allowedIps;
        this.allowedTimeStart = allowedTimeStart;
        this.allowedTimeEnd = allowedTimeEnd;
        this.authType = authType.getValue();
        this.authConfig = authConfig;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public LinkPolicy.AuthType getAuthTypeEnum() {
        return LinkPolicy.AuthType.fromValue(authType);
    }
    
    public void setAuthTypeEnum(LinkPolicy.AuthType authType) {
        this.authType = authType.getValue();
    }
}
