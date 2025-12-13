package com.quizgenix.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Required

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizgenix.model.Question;

@Service
// @SuppressWarnings("null") // Suppresses remaining IDE-specific null warnings
public class AiService {

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.model}")
    private String modelId;

    @SuppressWarnings("unchecked")
    public List<Question> generateQuestions(String topic, String difficulty, int count) {
        RestTemplate restTemplate = new RestTemplate();

        String prompt = String.format(
                "Generate %d multiple-choice questions on '%s' (%s level). " +
                        "Return JSON array ONLY. Do not include markdown formatting like ```json. " +
                        "Format: [{\"text\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correctAnswer\": \"A\", \"explanation\": \"...\"}]",
                count, topic, difficulty);

        // FIX 1: Use Generic Type <String, Object> to fix "Raw Type" warning
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // FIX 2: Objects.requireNonNull ensures apiKey is not null
        headers.set("Authorization", "Bearer " + Objects.requireNonNull(apiKey, "API Key must not be null"));

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // FIX 3: Objects.requireNonNull ensures URL and Method are not null
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    Objects.requireNonNull(apiUrl, "API URL is null"),
                    Objects.requireNonNull(HttpMethod.POST, "HTTP Method is null"),
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            // ... (Rest of logic remains the same) ...

            Map<String, Object> response = responseEntity.getBody();
            if (response == null || !response.containsKey("choices")) {
                throw new RuntimeException("AI returned an invalid response structure.");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String jsonString = (String) message.get("content");

            jsonString = jsonString.replaceAll("```json", "").replaceAll("```", "").trim();

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<Question>>() {
            });

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RuntimeException("Daily AI Quota Exceeded. Please try again tomorrow.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating quiz: " + e.getMessage());
        }
    }
}