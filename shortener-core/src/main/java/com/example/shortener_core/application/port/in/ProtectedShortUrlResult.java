package com.example.shortener_core.application.port.in;

import com.example.shortener_core.domain.model.LinkPolicy;
import com.example.shortener_core.domain.model.ShortUrl;

public record ProtectedShortUrlResult(
        ShortUrl shortUrl,
        LinkPolicy policy
) {
}
