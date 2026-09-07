package com.example.urlshortener.web;

import com.example.urlshortener.application.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ShortUrlNotFoundException.class)
    ResponseEntity<ApiError> notFound(RuntimeException e, HttpServletRequest r) {
        return error(HttpStatus.NOT_FOUND, "SHORT_URL_NOT_FOUND", e.getMessage(), r, Map.of());
    }

    @ExceptionHandler(ShortUrlExpiredException.class)
    ResponseEntity<ApiError> expired(RuntimeException e, HttpServletRequest r) {
        return error(HttpStatus.GONE, "SHORT_URL_EXPIRED", e.getMessage(), r, Map.of());
    }

    @ExceptionHandler(DuplicateShortCodeException.class)
    ResponseEntity<ApiError> duplicate(RuntimeException e, HttpServletRequest r) {
        return error(HttpStatus.CONFLICT, "SHORT_CODE_DUPLICATE", e.getMessage(), r, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalid(RuntimeException e, HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage(), r, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        var fields = e.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(f -> f.getField(), f -> Optional.ofNullable(f.getDefaultMessage()).orElse("invalid"), (a, b) -> a, LinkedHashMap::new));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", r, fields);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpServletRequest r) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Malformed JSON or invalid field format", r, Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI(), fields));
    }
}
