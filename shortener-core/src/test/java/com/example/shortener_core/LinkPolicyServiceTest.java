package com.example.shortener_core;

import com.example.shortener_core.application.port.event.LinkPolicyCreatedEvent;
import com.example.shortener_core.application.port.out.IdGenerator;
import com.example.shortener_core.application.port.out.LinkPolicyCachePort;
import com.example.shortener_core.application.port.out.LinkPolicyRepositoryPort;
import com.example.shortener_core.application.port.out.UrlRepositoryPort;
import com.example.shortener_core.application.service.LinkPolicyService;
import com.example.shortener_core.common.exception.NotSupportedAuthException;
import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.domain.model.ShortUrl;
import com.example.shortener_core.domain.valueobject.LongUrl;
import com.example.shortener_core.domain.valueobject.ShortCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LinkPolicyServiceTest {

    @Mock
    private UrlRepositoryPort urlRepository;

    @Mock
    private LinkPolicyRepositoryPort repository;

    @Mock
    private LinkPolicyCachePort cache;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private LinkPolicyService service;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void createPolicy_whenAuthTypeIsBasic_throwsExceptionWithoutAccessingInfrastructure() {
        Instant allowedTimeEnd = Instant.now().plusSeconds(3600);

        // Act + Assert
        assertThrows(
                NotSupportedAuthException.class,
                () -> service.createPolicy(
                        1L,
                        "abc123",
                        List.of(),
                        null,
                        allowedTimeEnd,
                        LinkPolicy.AuthType.BASIC
                )
        );

        verifyNoInteractions(repository, cache, idGenerator, eventPublisher, urlRepository);
    }

    @Test
    void createPolicy_whenAuthTypeIsNull_throwsExceptionWithoutAccessingInfrastructure() {
        Instant allowedTimeEnd = Instant.now().plusSeconds(3600);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createPolicy(
                        1L,
                        "abc123",
                        List.of(),
                        null,
                        allowedTimeEnd,
                        null
                )
        );

        verifyNoInteractions(repository, cache, idGenerator, eventPublisher, urlRepository);

    }


    @Test
    void createPolicy_whenInputIsValid_savesAndReturnsPolicy() {
        // Arrange
        Long linkId = 1L;
        String shortcode = "abc123";

        ShortUrl shortUrl = new ShortUrl(
                linkId,
                ShortCode.of(shortcode),
                LongUrl.of("https://example.com"),
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        when(repository.existsByShortcode(shortcode))
                .thenReturn(false);

        when(repository.existsByLinkId(linkId))
                .thenReturn(false);

        when(idGenerator.nextId())
                .thenReturn(42L);

        when(urlRepository.findByShortCode(shortcode))
                .thenReturn(Optional.of(shortUrl));

        when(repository.save(any(LinkPolicy.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0, LinkPolicy.class)
                );

        Instant allowedTimeEnd = Instant.now().plusSeconds(3600);


        // Act
        LinkPolicy result = service.createPolicy(
                linkId,
                shortcode,
                List.of("127.0.0.1"),
                null,
                allowedTimeEnd,
                LinkPolicy.AuthType.NONE
        );

        // Assert
        assertEquals(42L, result.getId());
        assertEquals(linkId, result.getLinkId());
        assertEquals(shortcode, result.getShortcodeValue());
        assertEquals(LinkPolicy.AuthType.NONE, result.getAuthType());

        verify(repository).existsByShortcode(shortcode);
        verify(repository).existsByLinkId(linkId);
        verify(idGenerator).nextId();
        verify(repository).save(any(LinkPolicy.class));


        ArgumentCaptor<LinkPolicyCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(LinkPolicyCreatedEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertSame(result, eventCaptor.getValue().linkPolicy());

        verifyNoInteractions(cache);
    }


    @Test
    void createPolicy_whenShortcodeAlreadyHasPolicy_throwsExceptionAndStopsProcessing(){
        Long linkId = 1L;
        String shortcode = "abc123";

        when(urlRepository.findByShortCode(shortcode))
                .thenReturn(Optional.of(new ShortUrl(
                        linkId,
                        ShortCode.of(shortcode),
                        LongUrl.of("https://example.com"),
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                )));

        when(repository.existsByShortcode(shortcode))
                .thenReturn(true);

        Instant allowedTimeEnd = Instant.now().plusSeconds(3600);


        assertThrows(IllegalArgumentException.class, () -> service.createPolicy(
                linkId,
                shortcode,
                List.of("127.0.0.1"),
                null,
                allowedTimeEnd,
                LinkPolicy.AuthType.NONE));

        verify(repository).existsByShortcode("abc123");
        verify(repository, never()).existsByLinkId(anyLong());
        verifyNoInteractions(cache, idGenerator, eventPublisher);
    }

    @Test
    void updatePolicy_whenAuthTypeIsBasic_throwsWithoutAccessingInfrastructure(){
        Instant allowedTimeEnd = Instant.now().plusSeconds(3600);

        LinkPolicy policy = new LinkPolicy(
                42L,
                1L,
                ShortCode.of("abc123"),
                List.of(),
                null,
                allowedTimeEnd,
                LinkPolicy.AuthType.BASIC,
                Instant.now(),
                Instant.now()
        );

        assertThrows(
                NotSupportedAuthException.class,
                () -> service.updatePolicy(policy)
        );

        verifyNoInteractions(repository, cache, idGenerator, eventPublisher, urlRepository);

    }


    @Test
    void updatePolicy_whenPolicyIsNull_throwsWithoutAccessingInfrastructure(){
        LinkPolicy policy = null;

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updatePolicy(policy)
        );
        verifyNoInteractions(repository, cache, idGenerator, eventPublisher, urlRepository);
    }


    @Test
    void createPolicy_whenLinkIdDoesNotBelongToShortcode_throwsWithoutCreatingPolicy(){
        Long linkId = 1L;
        String shortcode = "abc123";

        ShortUrl existingUrl = new ShortUrl(
                2L,
                ShortCode.of(shortcode),
                LongUrl.of("https://example.com"),
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        when(urlRepository.findByShortCode(shortcode))
                .thenReturn(Optional.of(existingUrl));

        assertThrows(IllegalArgumentException.class, () -> service.createPolicy(
                linkId, shortcode, List.of(), null, Instant.now(), LinkPolicy.AuthType.NONE)
        );

        verifyNoInteractions(repository, idGenerator, eventPublisher);
    }

}
