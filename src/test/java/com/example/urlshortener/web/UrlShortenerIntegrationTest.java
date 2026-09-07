package com.example.urlshortener.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @Test
    void rejectsOversizedUrlsReservedAliasesAndMalformedJson() throws Exception {
        for (String body : new String[]{
                mapper.writeValueAsString(java.util.Map.of("url", "https://example.com/" + "a".repeat(2048))),
                "{\"url\":\"https://example.com\",\"customAlias\":\"actuator\"}",
                "{broken json"}) {
            mvc.perform(post("/api/v1/urls").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }


    @Test
    void createsRedirectsCountsVisitsAndDeletesUsingH2() throws Exception {
        var response = mvc.perform(post("/api/v1/urls")
                        .contentType("application/json")
                        .content("{\"url\":\"https://example.com/long/path\"}"))
                .andExpect(status().isCreated()).andReturn();
        String code = mapper.readTree(response.getResponse().getContentAsString()).get("shortCode").asText();
        mvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/long/path"));
        mvc.perform(get("/api/v1/urls/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessCount").value(1));
        mvc.perform(delete("/api/v1/urls/" + code)).andExpect(status().isNoContent());
        mvc.perform(get("/" + code)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateAlias() throws Exception {
        String body = "{\"url\":\"https://example.com\",\"customAlias\":\"myalias\"}";
        mvc.perform(post("/api/v1/urls").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/urls").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidUrlsAndPastExpiration() throws Exception {
        for (String body : new String[]{
                "{\"url\":\"javascript:alert(1)\"}",
                "{\"url\":\"/relative\"}",
                "{\"url\":\"https://example.com\",\"expiresAt\":\"2000-01-01T00:00:00Z\"}",
                "{}"}) {
            mvc.perform(post("/api/v1/urls").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
