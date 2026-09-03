package com.example.shortener_core.application.port.in;

import com.example.shortener_core.domain.model.LinkPolicy;

import java.time.Instant;
import java.util.List;

public interface CreateProtectedShortUrlUseCase {

    ProtectedShortUrlResult createProtectedShortUrl(String longUrl, Instant expireAt,
                                    List<String> allowedIps,
                                    Instant allowedTimeStart,
                                    LinkPolicy.AuthType authType);

}
