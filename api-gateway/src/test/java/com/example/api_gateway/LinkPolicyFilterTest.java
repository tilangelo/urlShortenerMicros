package com.example.api_gateway;

import com.example.api_gateway.filter.LinkPolicyFilter;
import com.example.api_gateway.model.LinkPolicy;
import com.example.api_gateway.model.exception.UnsupportedAuthenticationTypeException;
import com.example.api_gateway.service.AuthValidationService;
import com.example.api_gateway.service.LinkPolicyService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LinkPolicyFilterTest {

    private LinkPolicyService linkPolicyService;
    private AuthValidationService authValidationService;
    private GatewayFilterChain chain;
    private LinkPolicyFilter filter;

    @BeforeEach
    void setUp() {
        linkPolicyService = mock(LinkPolicyService.class);
        authValidationService = mock(AuthValidationService.class);
        chain = mock(GatewayFilterChain.class);

        CircuitBreaker circuitBreaker =
                CircuitBreaker.ofDefaults("link-policy-filter-test");

        filter = new LinkPolicyFilter(
                linkPolicyService,
                authValidationService,
                circuitBreaker
        );
    }

    @Test
    void filter_whenAuthenticationTypeIsUnsupported_returns503WithoutCallingChain() {
        LinkPolicy policy = LinkPolicy.builder()
                .auth_type("basic")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/abc123")
                        .remoteAddress(
                                new InetSocketAddress(
                                        "127.0.0.1",
                                        12345
                                )
                        )
                        .build()
        );

        when(linkPolicyService.shortcodeExistsInRedis("abc123"))
                .thenReturn(Mono.just(true));

        when(linkPolicyService.getPolicy("abc123"))
                .thenReturn(Mono.just(policy));

        when(authValidationService.validateAuth(
                eq("basic"),
                any(ServerHttpRequest.class),
                eq("127.0.0.1")
        )).thenReturn(
                Mono.error(
                        new UnsupportedAuthenticationTypeException(
                                "Basic authentication is not supported"
                        )
                )
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exchange.getResponse().getStatusCode()
        );

        verify(chain, never()).filter(any());
    }
}
