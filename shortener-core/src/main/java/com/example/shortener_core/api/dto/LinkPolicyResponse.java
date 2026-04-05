package com.example.shortener_core.api.dto;

import com.example.shortener_core.domain.model.LinkPolicyRedis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkPolicyResponse {
    
    private List<String> allowed_ips;
    private Instant time_start;
    private Instant time_end;
    private String auth_type;
    private LinkPolicyResponse.AuthConfig auth_config;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthConfig {
        private String sso_endpoint;
        private String api_key_header;
        private String jwt_secret_key;
        private String basic_realm;
    }
    
    public static LinkPolicyResponse fromRedis(LinkPolicyRedis redis) {
        if (redis == null) {
            return null;
        }
        
        AuthConfig authConfig = null;
        if (redis.getAuth_config() != null) {
            authConfig = AuthConfig.builder()
                    .sso_endpoint(redis.getAuth_config().getSso_endpoint())
                    .api_key_header(redis.getAuth_config().getApi_key_header())
                    .jwt_secret_key(redis.getAuth_config().getJwt_secret_key())
                    .basic_realm(redis.getAuth_config().getBasic_realm())
                    .build();
        }
        
        return LinkPolicyResponse.builder()
                .allowed_ips(redis.getAllowed_ips())
                .time_start(redis.getTime_start())
                .time_end(redis.getTime_end())
                .auth_type(redis.getAuth_type())
                .auth_config(authConfig)
                .build();
    }
}
