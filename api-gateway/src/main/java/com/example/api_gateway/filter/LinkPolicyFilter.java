package com.example.api_gateway.filter;

import com.example.api_gateway.model.LinkPolicy;
import com.example.api_gateway.service.AuthValidationService;
import com.example.api_gateway.service.LinkPolicyService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkPolicyFilter implements GlobalFilter, Ordered {
    
    private final LinkPolicyService linkPolicyService;
    private final AuthValidationService authValidationService;
    
    // Metrics
    private final Counter accessDeniedTotal = Metrics.counter("access_denied_total");
    private final Counter clicksTotal = Metrics.counter("clicks_total");
    private final Counter redirectSuccessTotal = Metrics.counter("redirect_success_total");
    private final Timer requestLatency = Metrics.timer("request_latency_seconds");
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant startTime = Instant.now();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        // Извлекаем shortcode из пути
        String path = request.getPath().value();
        String shortcode = extractShortcode(path);
        
        if (shortcode == null || shortcode.isEmpty()) {
            return chain.filter(exchange);
        }
        
        log.debug("Processing request for shortcode: {}", shortcode);
        
        return linkPolicyService.getPolicy(shortcode)
                .flatMap(policy -> validateAndProcess(policy, exchange, chain, startTime))
                .switchIfEmpty(
                    // Политика не найдена, разрешаем доступ
                    Mono.defer(() -> {
                        clicksTotal.increment();
                        return chain.filter(exchange);
                    })
                );
    }
    
    private Mono<Void> validateAndProcess(LinkPolicy policy, ServerWebExchange exchange, 
                                         GatewayFilterChain chain, Instant startTime) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = getClientIp(request);
        String authHeader = request.getHeaders().getFirst("Authorization");
        
        // Проверяем временное окно
        if (!policy.isTimeWindowValid()) {
            log.warn("Time window validation failed for client IP: {}", clientIp);
            accessDeniedTotal.increment();
            return sendErrorResponse(exchange.getResponse(), HttpStatus.FORBIDDEN, "Access denied: Time window restriction");
        }
        
        // Проверяем ip пул
        if (!policy.isIpAllowed(clientIp)) {
            log.warn("IP validation failed for client IP: {}", clientIp);
            accessDeniedTotal.increment();
            return sendErrorResponse(exchange.getResponse(), HttpStatus.FORBIDDEN, "Access denied: IP not allowed");
        }
        
        // Проверяем аутентификацию
        return authValidationService.validateAuth(policy.getAuth_type(), policy.getAuth_config(), 
                                                 authHeader, clientIp)
                .flatMap(authValid -> {
                    if (!authValid) {
                        log.warn("Authentication failed for client IP: {}", clientIp);
                        accessDeniedTotal.increment();
                        return sendErrorResponse(exchange.getResponse(), HttpStatus.UNAUTHORIZED, "Access denied: Authentication failed");
                    }
                    
                    // Все проверки пройдены - увеличиваем метрики
                    clicksTotal.increment();
                    log.debug("Access granted for shortcode: {}, client IP: {}", 
                            extractShortcode(exchange.getRequest().getPath().value()), clientIp);
                    
                    return chain.filter(exchange)
                            .doOnSuccess(aVoid -> {
                                // Логируем успешный редирект
                                Duration latency = Duration.between(startTime, Instant.now());
                                requestLatency.record(latency);
                                redirectSuccessTotal.increment();
                                log.debug("Redirect completed in {} ms", latency.toMillis());
                            });
                });
    }
    
    private String extractShortcode(String path) {
        // Удаляем начальный слэш и извлекаем shortcode
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // Извлекаем первый сегмент пути как shortcode
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[0] : null;
    }
    
    private String getClientIp(ServerHttpRequest request) {
        // Пытаемся получить реальный ip из заголовков
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Резервный вариант: удалённый адрес
        return request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
    
    private Mono<Void> sendErrorResponse(ServerHttpResponse response, HttpStatus status, String message) {
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"error\": \"%s\", \"message\": \"%s\"}", 
                                   status.getReasonPhrase(), message);
        
        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -10; // run early в цепочке фильтров
    }
}
