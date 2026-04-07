package com.example.api_gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class Resilience4jConfig {

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)                    // 50% отказов
                .waitDurationInOpenState(Duration.ofSeconds(30))  // 30 секунд в открытом состоянии
                .slidingWindowSize(100)                         // 100 запросов в скользящем окне
                .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(10)                       // Минимум 10 вызовов для расчета
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordException(throwable -> !(throwable instanceof IllegalArgumentException))
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker("linkPolicyCircuitBreaker");
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.info("Circuit breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState()))
                .onFailureRateExceeded(event -> 
                    log.warn("Circuit breaker failure rate exceeded: {}%", event.getFailureRate()))
                .onCallNotPermitted(event -> 
                    log.warn("Circuit breaker call not permitted"));
        
        return registry;
    }

    @Bean
    public CircuitBreaker linkPolicyCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("linkPolicyCircuitBreaker");
    }
}
