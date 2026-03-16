package com.shortenerSoft.shortener_redirect.application.exception;

public class LinkDoesNotExistException extends RuntimeException {
    public LinkDoesNotExistException(String message) {
        super(message);
    }
}
