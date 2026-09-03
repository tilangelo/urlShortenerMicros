package com.example.api_gateway.model.exception;

public class CoreServiceException extends RuntimeException {
    public CoreServiceException(String message) {
        super(message);
    }

    public CoreServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
