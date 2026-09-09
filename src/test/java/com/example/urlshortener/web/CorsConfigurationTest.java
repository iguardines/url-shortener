package com.example.urlshortener.web;

import com.example.urlshortener.application.ShortUrlService;
import com.example.urlshortener.infrastructure.config.CorsConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
@Import(CorsConfiguration.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173,https://frontend.example.com")
class CorsConfigurationTest {
    @Autowired MockMvc mvc;
    @MockitoBean ShortUrlService service;

    @Test void allowsConfiguredLocalCreation() throws Exception {
        mvc.perform(options("/api/v1/urls").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
    @Test void allowsExactFrontendDeletion() throws Exception {
        mvc.perform(options("/api/v1/urls/abc123").header("Origin", "https://frontend.example.com")
                .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://frontend.example.com"));
    }
    @Test void deniesUnknownOrigin() throws Exception {
        mvc.perform(options("/api/v1/urls").header("Origin", "https://untrusted.example.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void rejectsWildcardConfiguration() {
        assertThatThrownBy(() -> new CorsConfiguration("*")).isInstanceOf(IllegalArgumentException.class);
    }
}
