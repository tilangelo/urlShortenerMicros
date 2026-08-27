package com.example.shortener_core.application.service;

import com.example.shortener_core.application.port.in.CreateProtectedShortUrlUseCase;
import com.example.shortener_core.application.port.in.CreateShortUrlUseCase;
import com.example.shortener_core.application.port.in.ProtectedShortUrlResult;
import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.domain.model.ShortUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Создаёт короткую ссылку вместе с политикой доступа в одной транзакции.
 * Если политика не сохранится, создание ссылки тоже откатится.
 */
@Service
@Slf4j
public class CreateProtectedShortUrlService implements CreateProtectedShortUrlUseCase {

    private final CreateShortUrlUseCase createShortUrlUseCase;
    private final LinkPolicyService linkPolicyService;

    /**
     * Создаёт сервис из операций создания ссылки и политики.
     *
     * @param createShortUrlUseCase создание обычной короткой ссылки
     * @param linkPolicyService создание политики доступа
     */
    public CreateProtectedShortUrlService(CreateShortUrlUseCase createShortUrlUseCase, LinkPolicyService linkPolicyService) {
        this.createShortUrlUseCase = createShortUrlUseCase;
        this.linkPolicyService = linkPolicyService;
    }

    /**
     * Создаёт ссылку и привязанную к ней политику с общим временем окончания действия.
     *
     * @param longUrl исходный URL
     * @param expireAt момент окончания действия ссылки и политики
     * @param allowedIps список разрешённых IP
     * @param allowedTimeStart начало окна доступа, может отсутствовать
     * @param authType тип аутентификации
     * @return созданная ссылка вместе с политикой
     */
    @Override
    @Transactional
    public ProtectedShortUrlResult createProtectedShortUrl(String longUrl, Instant expireAt,
                                                           List<String> allowedIps,
                                                           Instant allowedTimeStart,
                                                           LinkPolicy.AuthType authType) {

        LinkPolicy.validateTimeWindow(allowedTimeStart, expireAt);

        ShortUrl shortUrl = createShortUrlUseCase.createShortUrl(longUrl, expireAt);

        LinkPolicy policy = linkPolicyService.createPolicy(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                allowedIps,
                allowedTimeStart,
                expireAt,
                authType
        );

        log.info("Created protected short URL with shortcode: {}", shortUrl.getShortCode());

        return new ProtectedShortUrlResult(shortUrl, policy);
    }

}
