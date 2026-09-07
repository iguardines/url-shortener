package com.example.urlshortener.web;

import com.example.urlshortener.application.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
// prueba de auto-deploy
@RestController
public class UrlController {
    private final ShortUrlService service;
    private final String baseUrl;

    UrlController(ShortUrlService service, @Value("${app.base-url}") String baseUrl) {
        this.service = service;
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    @Operation(summary = "Create a short URL")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping("/api/v1/urls")
    ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        var saved = service.create(new CreateShortUrlCommand(request.url(), request.customAlias(), request.expiresAt()));
        return ResponseEntity.created(URI.create(baseUrl + "/api/v1/urls/" + saved.shortCode())).body(ShortUrlResponse.from(saved, baseUrl));
    }

    @Operation(summary = "Redirect to the original URL")
    @GetMapping("/{shortCode:[A-Za-z0-9_-]{4,32}}")
    ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        return ResponseEntity.status(HttpStatus.FOUND).location(service.resolve(shortCode)).cacheControl(CacheControl.noStore()).build();
    }

    @Operation(summary = "Get link metadata")
    @GetMapping("/api/v1/urls/{shortCode}")
    ShortUrlResponse get(@PathVariable String shortCode) {
        return ShortUrlResponse.from(service.get(shortCode), baseUrl);
    }

    @Operation(summary = "Delete a short URL")
    @DeleteMapping("/api/v1/urls/{shortCode}")
    ResponseEntity<Void> delete(@PathVariable String shortCode) {
        service.delete(shortCode);
        return ResponseEntity.noContent().build();
    }
}
