package com.example.urlshortener.application.exception;

public class ShortUrlNotFoundException extends RuntimeException {
    public ShortUrlNotFoundException(String code) {
        super("Short URL not found: " + code);
    }
}
