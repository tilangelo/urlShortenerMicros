package com.example.api_gateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LinkPolicy implements Serializable {
    
    private List<String> allowed_ips;
    private Instant time_start;
    private Instant time_end;
    private String auth_type;
    private AuthConfig auth_config;
    
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuthConfig implements Serializable {
        private String sso_endpoint;
        private String api_key_header;
        private String jwt_secret_key;
        private String basic_realm;
    }
    
    public boolean isTimeWindowValid() {
        Instant now = Instant.now();
        
        if (time_start == null && time_end == null) {
            return true; // Нет временных ограничений
        }
        
        boolean afterStart = time_start == null || !now.isBefore(time_start);
        boolean beforeEnd = time_end == null || !now.isAfter(time_end);
        
        return afterStart && beforeEnd;
    }
    
    public boolean isIpAllowed(String clientIp) {
        if (allowed_ips == null || allowed_ips.isEmpty()) {
            return true; // Нет IP-ограничений
        }
        
        return allowed_ips.stream()
                .anyMatch(pattern -> matchesIpPattern(pattern, clientIp));
    }
    
    private boolean matchesIpPattern(String pattern, String clientIp) {
        // Простое сравнение IP - можно улучшить поддержкой CIDR
        if (pattern.contains("/")) {
            // CIDR нотация - упрощённая проверка
            String[] parts = pattern.split("/");
            String network = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            // Пока что просто проверяем, что в той же сети /24
            if (prefixLength >= 24) {
                String[] networkParts = network.split("\\.");
                String[] clientParts = clientIp.split("\\.");
                return networkParts[0].equals(clientParts[0]) && 
                       networkParts[1].equals(clientParts[1]) && 
                       networkParts[2].equals(clientParts[2]);
            }
        }
        
        // Точное совпадение или wildcard
        if (pattern.equals("*") || pattern.equals("0.0.0.0/0")) {
            return true;
        }
        
        return pattern.equals(clientIp);
    }
}
