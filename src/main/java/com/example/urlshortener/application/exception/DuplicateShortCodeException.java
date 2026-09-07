package com.example.urlshortener.application.exception;

public class DuplicateShortCodeException extends RuntimeException {
    public DuplicateShortCodeException(String code) {
        super("Short code already exists: " + code);
    }
}
