package com.example.api_gateway.model.exception;

public class CacheServiceException extends RuntimeException {
    public CacheServiceException(String message, Throwable cause) {
        super(message);
    }
}
