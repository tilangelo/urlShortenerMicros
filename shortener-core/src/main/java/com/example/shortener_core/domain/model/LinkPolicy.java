package com.example.shortener_core.domain.model;

import com.example.shortener_core.domain.valueobject.ShortCode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class LinkPolicy {
    
    private final Long id;
    private final Long linkId;
    private final ShortCode shortcode;
    private final List<String> allowedIps;
    private final Instant allowedTimeStart;
    private final Instant allowedTimeEnd;
    private final AuthType authType;
    private final String authConfig;
    private final Instant createdAt;
    private final Instant updatedAt;
    
    public LinkPolicy(Long id, Long linkId, ShortCode shortcode, 
                      List<String> allowedIps, Instant allowedTimeStart, 
                      Instant allowedTimeEnd, AuthType authType, 
                      String authConfig, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.linkId = linkId;
        this.shortcode = shortcode;
        this.allowedIps = allowedIps;
        this.allowedTimeStart = allowedTimeStart;
        this.allowedTimeEnd = allowedTimeEnd;
        this.authType = authType;
        this.authConfig = authConfig;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public static LinkPolicy create(Long id, Long linkId, ShortCode shortcode,
                                  List<String> allowedIps, Instant allowedTimeStart,
                                  Instant allowedTimeEnd, AuthType authType,
                                  String authConfig) {
        Instant now = Instant.now();
        return new LinkPolicy(
            id, linkId, shortcode, allowedIps, allowedTimeStart, 
            allowedTimeEnd, authType, authConfig, now, now
        );
    }
    
    public String getShortcodeValue() {
        return shortcode.getValue();
    }
    
    public boolean isTimeWindowValid(Instant now) {
        if (allowedTimeStart == null && allowedTimeEnd == null) {
            return true; // No time restrictions
        }
        
        boolean afterStart = allowedTimeStart == null || !now.isBefore(allowedTimeStart);
        boolean beforeEnd = allowedTimeEnd == null || !now.isAfter(allowedTimeEnd);
        
        return afterStart && beforeEnd;
    }
    
    public enum AuthType {
        NONE(null),
        CORPORATE_SSO("corporate_sso"),
        API_KEY("api_key"),
        JWT("jwt"),
        BASIC("basic");
        
        private final String value;
        
        AuthType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static AuthType fromValue(String value) {
            if (value == null) return NONE;
            for (AuthType type : values()) {
                if (type.value != null && type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown auth type: " + value);
        }
    }
}
