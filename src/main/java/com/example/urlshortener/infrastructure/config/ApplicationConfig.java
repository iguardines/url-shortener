package com.example.urlshortener.infrastructure.config;

import com.example.urlshortener.domain.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ShortCodeGenerator shortCodeGenerator() {
        return new RandomBase62ShortCodeGenerator();
    }
}
