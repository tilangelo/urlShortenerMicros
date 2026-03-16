package com.example.shortener_core.config;

import com.example.shortener_core.application.port.out.IdGenerator;
import com.example.shortener_core.infrastructure.id.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {
    @Bean
    public IdGenerator idGenerator(@Value("${machine.number:1}") long workerId) {
        return new SnowflakeIdGenerator(workerId);
    }
}
