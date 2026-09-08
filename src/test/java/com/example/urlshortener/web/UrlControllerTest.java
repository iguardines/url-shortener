package com.example.urlshortener.web;

import com.example.urlshortener.application.ShortUrlService;
import com.example.urlshortener.application.exception.*;
import com.example.urlshortener.domain.ShortUrl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
@org.springframework.test.context.ActiveProfiles("local")
class UrlControllerTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    ShortUrlService service;

    @Test
    void createsLink() throws Exception {
        when(service.create(any())).thenReturn(link());
        mvc.perform(post("/api/v1/urls").contentType("application/json").content("{\"url\":\"https://example.com/path\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.shortCode").value("abcD1234"));
    }

    @Test
    void redirects() throws Exception {
        when(service.resolve("abcD1234")).thenReturn(URI.create("https://example.com/path"));
        mvc.perform(get("/abcD1234")).andExpect(status().isFound()).andExpect(header().string("Location", "https://example.com/path"));
    }

    @Test
    void returnsNotFoundContract() throws Exception {
        when(service.get("missing1")).thenThrow(new ShortUrlNotFoundException("missing1"));
        mvc.perform(get("/api/v1/urls/missing1")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHORT_URL_NOT_FOUND"));
    }

    @Test
    void returnsGoneForExpiredRedirect() throws Exception {
        when(service.resolve("expired1")).thenThrow(new ShortUrlExpiredException("expired1"));
        mvc.perform(get("/expired1")).andExpect(status().isGone()).andExpect(jsonPath("$.code").value("SHORT_URL_EXPIRED"));
    }

    @Test
    void validatesSchemeAtDomainBoundary() throws Exception {
        when(service.create(any())).thenThrow(new IllegalArgumentException("Only HTTP and HTTPS URLs are allowed"));
        mvc.perform(post("/api/v1/urls").contentType("application/json").content("{\"url\":\"ftp://example.com/a\"}")).andExpect(status().isBadRequest());
    }

    private ShortUrl link() {
        return new ShortUrl(1L, "abcD1234", URI.create("https://example.com/path"), Instant.parse("2026-01-01T00:00:00Z"), null, 0);
    }
}
