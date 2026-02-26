package com.example.shortener_core.config;

import com.example.shortener_core.application.port.out.IdGenerator;
import com.example.shortener_core.infrastructure.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {
    @Bean
    public IdGenerator idGenerator() {
        // В будущем workerId можно брать из env
        long workerId = 1L;
        return new SnowflakeIdGenerator(workerId);
    }
}
