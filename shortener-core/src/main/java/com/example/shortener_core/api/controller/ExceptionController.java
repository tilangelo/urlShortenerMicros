package com.example.shortener_core.api.controller;

import com.example.shortener_core.common.exception.NotSupportedAuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionController {

    @ExceptionHandler(NotSupportedAuthException.class)
    public ResponseEntity<String> handleNotSupportedAuthException(NotSupportedAuthException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_CONTENT);
    }

}
