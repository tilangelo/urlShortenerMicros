package com.example.shortener_core.api.dto;

import com.example.shortener_core.domain.model.LinkPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreateLinkPolicyRequest {
    
    @NotNull(message = "ID ссылки обязателен")
    private Long linkId;
    
    @NotBlank(message = "Шорткод обязателен")
    private String shortcode;
    
    private List<String> allowedIps;
    
    private Instant allowedTimeStart;
    
    private Instant allowedTimeEnd;
    
    private LinkPolicy.AuthType authType = LinkPolicy.AuthType.NONE;
    
    private String authConfig;
}
