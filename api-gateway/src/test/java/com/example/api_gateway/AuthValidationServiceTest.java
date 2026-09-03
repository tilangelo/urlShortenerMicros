package com.example.api_gateway;

import com.example.api_gateway.model.exception.UnsupportedAuthenticationTypeException;
import com.example.api_gateway.service.AuthEndpointExchange;
import com.example.api_gateway.service.AuthValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthValidationServiceTest {

    @Mock
    private AuthEndpointExchange authEndpointExchange;

    @InjectMocks
    private AuthValidationService service;

    @Test
    void validateAuth_whenAuthTypeIsNull_allowsAccess() {
        ServerHttpRequest request =
                MockServerHttpRequest.get("/abc123").build();

        StepVerifier.create(
                        service.validateAuth(
                                null,
                                request,
                                "127.0.0.1"
                        )
                )
                .expectNext(true)
                .verifyComplete();

        verifyNoInteractions(authEndpointExchange);
    }


    @Test
    void validateAuth_whenAuthTypeIsBasic_emitsUnsupportedAuthenticationError() {
        ServerHttpRequest request = MockServerHttpRequest.get("/abc123")
                .header(HttpHeaders.AUTHORIZATION, "Basic Zm9vOmJhcggg==")
                .build();

        StepVerifier.create(service.validateAuth("basic", request, "127.0.0.1"))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(UnsupportedAuthenticationTypeException.class, error);
                })
                .verify();
        verifyNoInteractions(authEndpointExchange);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "null",
            "oauth2",
            "BASIC"
    })
    void validateAuth_whenAuthTypeIsUnknown_emitsUnsupportedAuthenticationError(String authType) {
        ServerHttpRequest request = MockServerHttpRequest.get("/abc123").build();

        StepVerifier.create(service.validateAuth(authType, request, "127.0.0.1"))
                .expectErrorSatisfies(error -> {
                    UnsupportedAuthenticationTypeException exception = assertInstanceOf(
                            UnsupportedAuthenticationTypeException.class, error
                    );
                    assertEquals("Unsupported authentication type: " + authType, exception.getMessage());
                }).verify();

        verifyNoInteractions(authEndpointExchange);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "corporate_sso",
            "api_key"
    })
    void validateAuth_whenAuthorizationHeaderIsMissing_returnsFalseWithoutCallingEndpoint(String authType) {
        ServerHttpRequest request = MockServerHttpRequest.get("/abc123")
                .build();

        StepVerifier.create(service.validateAuth(authType, request, "127.0.0.1"))
                .expectNext(false)
                .verifyComplete();

        verifyNoInteractions(authEndpointExchange);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "corporate_sso",
            "api_key"
    })
    void validateAuth_whenBearerCredentialIsBlank_returnsFalseWithoutCallingEndpoint(
            String authType
    ) {
        ServerHttpRequest request =
                MockServerHttpRequest.get("/abc123")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer    "
                        )
                        .build();

        Mono<Boolean> result = service.validateAuth(
                authType,
                request,
                "127.0.0.1"
        );

        assertNotNull(
                result,
                "validateAuth must always return Mono"
        );

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verifyNoInteractions(authEndpointExchange);
    }


    @Test
    void validateAuth_whenApiKeyIsPresent_returnsEndpointResult() {
        String apiKey = "abc";
        String clientIp = "127.0.0.1";

        ServerHttpRequest request =
                MockServerHttpRequest.get("/abc123")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + apiKey
                        )
                        .build();

        when(
                authEndpointExchange.apiKeyExchange(
                        apiKey,
                        clientIp,
                        Duration.ofSeconds(3)
                )
        ).thenReturn(Mono.just(true));

        StepVerifier.create(
                        service.validateAuth(
                                "api_key",
                                request,
                                clientIp
                        )
                )
                .expectNext(true)
                .verifyComplete();

        verify(authEndpointExchange).apiKeyExchange(
                apiKey,
                clientIp,
                Duration.ofSeconds(3)
        );
    }


}
