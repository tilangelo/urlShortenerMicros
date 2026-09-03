package com.example.api_gateway.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {

    @Bean("coreWebClient")
    public WebClient coreWebClient(WebClient.Builder builder,
                                   @Value("${gateway.core-service.base-url}") String baseUrl,
                                   @Value("${gateway.core-service.username}") String username,
                                   @Value("${gateway.core-service.password}") String password) {

        return builder
                .clone()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(256 * 1024))
                .build();

    }

    @Bean("authWebClient")
    public WebClient authWebClient(WebClient.Builder builder) {
        return builder
                .clone()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(64 * 1024))
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public JsonMapper jsonMapper() {
        JsonMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        mapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }
}
