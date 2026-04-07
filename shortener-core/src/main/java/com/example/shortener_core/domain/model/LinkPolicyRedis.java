package com.example.shortener_core.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkPolicyRedis implements Serializable {
    
    private List<String> allowed_ips;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant time_start;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant time_end;
    
    private String auth_type;
    private AuthConfig auth_config;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthConfig implements Serializable {
        private String sso_endpoint;
        private String api_key_header;
        private String jwt_secret_key;
        private String basic_realm;
    }
    
    public static LinkPolicyRedis fromDomain(LinkPolicy linkPolicy) {
        if (linkPolicy == null) {
            return null;
        }
        
        return LinkPolicyRedis.builder()
                .allowed_ips(linkPolicy.getAllowedIps())
                .time_start(linkPolicy.getAllowedTimeStart())
                .time_end(linkPolicy.getAllowedTimeEnd())
                .auth_type(linkPolicy.getAuthType().getValue())
                .auth_config(parseAuthConfig(linkPolicy.getAuthConfig(), linkPolicy.getAuthType()))
                .build();
    }
    
    private static AuthConfig parseAuthConfig(String authConfigJson, LinkPolicy.AuthType authType) {
        if (authConfigJson == null || authType == LinkPolicy.AuthType.NONE) {
            return null;
        }
        
        // Пока что возвращает простую конфигурацию — в дальнейшем корректно обрабатывать JSON.
        return AuthConfig.builder().build();
    }
}
