package com.example.shortener_core.common.exception;

public class OutOfTimeException extends RuntimeException {
    public OutOfTimeException(String message) {
        super(message);
    }
}
