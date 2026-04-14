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

    public static LinkPolicyResponse fromRedis(LinkPolicyRedis redis) {
        if (redis == null) {
            return null;
        }
        
        return LinkPolicyResponse.builder()
                .allowed_ips(redis.getAllowed_ips())
                .time_start(redis.getTime_start())
                .time_end(redis.getTime_end())
                .auth_type(redis.getAuth_type())
                .build();
    }
}
