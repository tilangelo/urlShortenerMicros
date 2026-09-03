package com.example.shortener_core;

import com.example.shortener_core.application.port.event.LinkPolicyCacheListener;
import com.example.shortener_core.application.port.event.LinkPolicyCreatedEvent;
import com.example.shortener_core.application.port.event.LinkPolicyDeletedEvent;
import com.example.shortener_core.application.port.event.LinkPolicyUpdatedEvent;
import com.example.shortener_core.application.port.out.LinkPolicyCachePort;
import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.domain.model.LinkPolicyRedis;
import com.example.shortener_core.domain.valueobject.ShortCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LinkPolicyCacheListenerTest {

    @Mock
    private LinkPolicyCachePort cachePort;

    @InjectMocks
    private LinkPolicyCacheListener listener;

    @Test
    void handleCreatedLinkPolicy_whenPolicyIsActive_savesPolicyWithRemainingTtl() {
        // Arrange
        Instant allowedTimeEnd = Instant.now().plus(Duration.ofHours(1));

        LinkPolicy policy = LinkPolicy.create(
                42L,
                1L,
                ShortCode.of("abc123"),
                List.of("127.0.0.1"),
                null,
                allowedTimeEnd,
                LinkPolicy.AuthType.NONE
        );

        LinkPolicyCreatedEvent event = new LinkPolicyCreatedEvent(policy);

        // Act
        listener.handleCreatedLinkPolicy(event);

        // Assert
        ArgumentCaptor<LinkPolicyRedis> policyCaptor =
                ArgumentCaptor.forClass(LinkPolicyRedis.class);

        ArgumentCaptor<Duration> durationCaptor =
                ArgumentCaptor.forClass(Duration.class);

        verify(cachePort).savePolicy(
                eq("abc123"),
                policyCaptor.capture(),
                durationCaptor.capture()
        );

        assertEquals(policy.getAllowedIps(), policyCaptor.getValue().getAllowed_ips());
        assertEquals(policy.getAllowedTimeEnd(), policyCaptor.getValue().getTime_end());
        Duration ttl = durationCaptor.getValue();
        assertTrue(ttl.compareTo(Duration.ZERO) > 0);
        assertTrue(ttl.compareTo(Duration.ofHours(1)) <= 0);

    }


    @Test
    void handleCreatedLinkPolicy_whenPolicyIsExpired_doesNotSavePolicy() {
        Instant allowedTimeEnd = Instant.now().minusSeconds(1);

        LinkPolicy policy = LinkPolicy.create(
                42L,
                1L,
                ShortCode.of("abc123"),
                List.of("127.0.0.1"),
                null,
                allowedTimeEnd,
                LinkPolicy.AuthType.NONE
        );

        LinkPolicyCreatedEvent event = new LinkPolicyCreatedEvent(policy);


        listener.handleCreatedLinkPolicy(event);


        verifyNoInteractions(cachePort);

    }


    @Test
    void handleCreatedLinkPolicy_whenCacheFails_doesNotPropagateException(){
        Instant allowedTimeEnd = Instant.now().plus(Duration.ofHours(1));

        LinkPolicy policy = LinkPolicy.create(
                42L,
                1L,
                ShortCode.of("abc123"),
                List.of("127.0.0.1"),
                null,
                allowedTimeEnd,
                LinkPolicy.AuthType.NONE
        );

        LinkPolicyCreatedEvent event = new LinkPolicyCreatedEvent(policy);


        doThrow(new RuntimeException("Redis unavailable"))
                .when(cachePort)
                .savePolicy(
                        anyString(),
                        any(LinkPolicyRedis.class),
                        any(Duration.class)
                );


        assertDoesNotThrow(
                () -> listener.handleCreatedLinkPolicy(event)
        );


        verify(cachePort).savePolicy(
                eq("abc123"),
                any(LinkPolicyRedis.class),
                any(Duration.class)
        );
    }


    @Test
    void handleDeletedLinkPolicy_whenEventReceived_deletesCachedPolicy(){
        String code = "abc123";

        LinkPolicyDeletedEvent event = new LinkPolicyDeletedEvent(code);


        listener.handleDeletedLinkPolicy(event);


        verify(cachePort).deletePolicy(code);
    }


    @Test
    void handleDeletedLinkPolicy_whenCacheFails_doesNotPropagateException(){
        String code = "abc123";
        LinkPolicyDeletedEvent event = new LinkPolicyDeletedEvent(code);

        doThrow(new RuntimeException("Redis unavailable"))
                .when(cachePort)
                .deletePolicy(code);



        assertDoesNotThrow(
                () -> listener.handleDeletedLinkPolicy(event)
        );

        verify(cachePort).deletePolicy("abc123");

    }


    @Test
    void handleUpdatedLinkPolicy_whenPolicyIsActive_updatesCachedPolicy() {
        Instant allowedTimeEnd = Instant.now().plus(Duration.ofHours(1));

        LinkPolicy policy = LinkPolicy.create(
                42L,
                1L,
                ShortCode.of("abc123"),
                List.of("127.0.0.1"),
                null,
                allowedTimeEnd,
                LinkPolicy.AuthType.NONE
        );

        LinkPolicyUpdatedEvent event = new LinkPolicyUpdatedEvent(policy);


        listener.handleUpdatedLinkPolicy(event);


        verify(cachePort).savePolicy(
                eq("abc123"),
                any(LinkPolicyRedis.class),
                any(Duration.class)
        );
    }

}
