package com.example.shortener_core.common.exception;

public class NotSupportedAuthException extends RuntimeException {
    public NotSupportedAuthException(String message) {
        super(message);
    }
}
