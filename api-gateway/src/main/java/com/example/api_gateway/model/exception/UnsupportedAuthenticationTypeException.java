package com.example.api_gateway.model.exception;

public class UnsupportedAuthenticationTypeException extends RuntimeException {
    public UnsupportedAuthenticationTypeException(String message) {
        super(message);
    }
}
