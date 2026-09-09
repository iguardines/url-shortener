package com.example.urlshortener.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.net.URI;
import java.util.Arrays;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {
    private final String[] origins;

    public CorsConfiguration(@Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toArray(String[]::new);
        for (String origin : origins) {
            URI uri = URI.create(origin);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || (uri.getPath() != null && !uri.getPath().isEmpty())) {
                throw new IllegalArgumentException("CORS origins must be exact HTTP(S) origins without paths or wildcards");
            }
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origins.length == 0) return;
        registry.addMapping("/api/v1/urls/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
