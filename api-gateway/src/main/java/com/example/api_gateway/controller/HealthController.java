package com.example.api_gateway.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController implements HealthIndicator {

    private final CircuitBreaker linkPolicyCircuitBreaker;

    @GetMapping("/health/detailed")
    public Mono<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        
        CircuitBreaker.State state = linkPolicyCircuitBreaker.getState();
        CircuitBreaker.Metrics metrics = linkPolicyCircuitBreaker.getMetrics();
        
        health.put("circuitBreaker", Map.of(
                "state", state.name(),
                "failureRate", metrics.getFailureRate(),
                "numberOfBufferedCalls", metrics.getNumberOfBufferedCalls(),
                "numberOfFailedCalls", metrics.getNumberOfFailedCalls(),
                "numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls(),
                "numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls()
        ));
        
        health.put("status", state == CircuitBreaker.State.OPEN ? "DOWN" : "UP");
        health.put("timestamp", System.currentTimeMillis());
        
        return Mono.just(health);
    }

    @Override
    public Health health() {
        CircuitBreaker.State state = linkPolicyCircuitBreaker.getState();
        CircuitBreaker.Metrics metrics = linkPolicyCircuitBreaker.getMetrics();
        
        if (state == CircuitBreaker.State.OPEN) {
            return Health.down()
                    .withDetail("circuitBreaker", "OPEN")
                    .withDetail("failureRate", metrics.getFailureRate())
                    .withDetail("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls())
                    .build();
        }
        
        return Health.up()
                .withDetail("circuitBreaker", state.name())
                .withDetail("failureRate", metrics.getFailureRate())
                .withDetail("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls())
                .build();
    }
}
