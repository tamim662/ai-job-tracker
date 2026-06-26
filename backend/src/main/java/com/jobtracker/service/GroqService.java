package com.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroqService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Value("${app.groq.api-key}")
    private String apiKey;

    @Value("${app.groq.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String call(String systemPrompt, String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 2048,
                "temperature", 0.1
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String response = restTemplate.postForObject(GROQ_URL, request, String.class);

        try {
            JsonNode json = objectMapper.readTree(response);
            return json.at("/choices/0/message/content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Groq response", e);
        }
    }
}
