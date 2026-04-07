package com.example.shortener_core.domain.valueobject;

import java.net.URI;
import java.net.URISyntaxException;

public class LongUrl {
    private final String value;

    private LongUrl(String value) {
        this.value = validateAndNormalize(value);
    }

    public static LongUrl of(String url) {
        return new LongUrl(url);
    }

    private String validateAndNormalize(String url) {
        try {
            URI uri = new URI(url);

            // Добавляем схему по умолчанию (https)
            if (uri.getScheme() == null) {
                uri = new URI("https://" + url);
            }

            if (!uri.getScheme().matches("https?|ftp")) {
                throw new IllegalArgumentException("Invalid URL scheme");
            }

            uri = uri.normalize();

            // canonicalize — последний шаг
            return canonicalize(uri);

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    private String canonicalize(URI uri) throws URISyntaxException {
        String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();
        return new URI(
                uri.getScheme().toLowerCase(),
                uri.getAuthority().toLowerCase(),
                path,
                uri.getQuery(),
                null
        ).toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LongUrl)) return false;
        return value.equals(((LongUrl) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public String getValue() {
        return value;
    }
}
