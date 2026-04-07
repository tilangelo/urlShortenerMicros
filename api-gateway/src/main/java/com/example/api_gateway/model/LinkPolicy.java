package com.example.api_gateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class LinkPolicy implements Serializable {
    
    private List<String> allowed_ips;
    private Instant time_start;
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
        
        // разрешаю localhost
        if (clientIp.equals("127.0.0.1") || clientIp.equals("localhost") || 
            clientIp.equals("0:0:0:0:0:0:0:1") || clientIp.equals("::1")) {
            return true;
        }
        
        return allowed_ips.stream()
                .anyMatch(pattern -> matchesIpPattern(pattern, clientIp));
    }
    
    private boolean matchesIpPattern(String pattern, String clientIp) {
        // Поддержка CIDR нотации (например, 192.168.1.0/24)
        if (pattern.contains("/")) {
            return isIpInCidrRange(clientIp, pattern);
        }
        
        // Поддержка диапазонов через дефис (например, 192.168.1.1-192.168.1.50)
        if (pattern.contains("-")) {
            return isIpInRange(clientIp, pattern);
        }
        
        // Поддержка wildcard (например, 192.168.1.*)
        if (pattern.contains("*")) {
            return matchesWildcard(pattern, clientIp);
        }
        
        // Точное совпадение
        if (pattern.equals("0.0.0.0/0")) {
            return true; // Любой IP
        }
        
        return pattern.equals(clientIp);
    }
    
    private boolean isIpInCidrRange(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            String[] networkParts = network.split("\\.");
            String[] ipParts = ip.split("\\.");
            
            // Преобразуем IP адреса в числа
            long networkAddr = ((Long.parseLong(networkParts[0]) << 24) |
                               (Long.parseLong(networkParts[1]) << 16) |
                               (Long.parseLong(networkParts[2]) << 8) |
                               Long.parseLong(networkParts[3])) & (0xFFFFFFFFL);
            
            long ipAddr = ((Long.parseLong(ipParts[0]) << 24) |
                          (Long.parseLong(ipParts[1]) << 16) |
                          (Long.parseLong(ipParts[2]) << 8) |
                          Long.parseLong(ipParts[3])) & (0xFFFFFFFFL);
            
            long mask = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
            
            return (networkAddr & mask) == (ipAddr & mask);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isIpInRange(String ip, String range) {
        try {
            String[] parts = range.split("-");
            String startIp = parts[0].trim();
            String endIp = parts[1].trim();
            
            long ipValue = ipToLong(ip);
            long startValue = ipToLong(startIp);
            long endValue = ipToLong(endIp);
            
            return ipValue >= startValue && ipValue <= endValue;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean matchesWildcard(String pattern, String ip) {
        String[] patternParts = pattern.split("\\.");
        String[] ipParts = ip.split("\\.");
        
        if (patternParts.length != ipParts.length) {
            return false;
        }
        
        for (int i = 0; i < patternParts.length; i++) {
            if (!patternParts[i].equals("*") && !patternParts[i].equals(ipParts[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        return ((Long.parseLong(parts[0]) << 24) |
                (Long.parseLong(parts[1]) << 16) |
                (Long.parseLong(parts[2]) << 8) |
                Long.parseLong(parts[3])) & 0xFFFFFFFFL;
    }
}
