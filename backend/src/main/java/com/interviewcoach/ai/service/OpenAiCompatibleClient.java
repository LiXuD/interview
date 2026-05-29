package com.interviewcoach.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleClient {

    private static final String MODE_CHAT_COMPLETIONS = "chatCompletions";
    private static final String MODE_RESPONSES = "responses";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateJson(String baseUrl, String apiKey, String model,
                               String openaiApiMode, String systemPrompt, String userPrompt) {
        if (MODE_RESPONSES.equals(openaiApiMode)) {
            return callResponsesApi(baseUrl, apiKey, model, systemPrompt, userPrompt);
        } else {
            return callChatCompletionsApi(baseUrl, apiKey, model, systemPrompt, userPrompt);
        }
    }

    private String callChatCompletionsApi(String baseUrl, String apiKey, String model,
                                          String systemPrompt, String userPrompt) {
        String url = normalizeUrl(baseUrl) + "chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            return response.getBody().path("choices").get(0).path("message").path("content").asText();
        } catch (Exception ex) {
            throw new IllegalStateException(providerCallFailure("chatCompletions", model, MODE_CHAT_COMPLETIONS), ex);
        }
    }

    private String callResponsesApi(String baseUrl, String apiKey, String model,
                                    String systemPrompt, String userPrompt) {
        String url = normalizeUrl(baseUrl) + "responses";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", systemPrompt + "\n\n" + userPrompt
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            JsonNode output = response.getBody().path("output");
            if (output.isArray() && output.size() > 0) {
                JsonNode content = output.get(0).path("content");
                if (content.isArray() && content.size() > 0) {
                    return content.get(0).path("text").asText();
                }
            }
            throw new IllegalStateException("Unexpected responses API output structure");
        } catch (Exception ex) {
            throw new IllegalStateException(providerCallFailure("responses", model, MODE_RESPONSES), ex);
        }
    }

    public void testConnection(String baseUrl, String apiKey, String model, String openaiApiMode) {
        generateJson(baseUrl, apiKey, model, openaiApiMode,
                "Reply with valid JSON: {\"ok\":true}",
                "Say ok.");
    }

    public List<String> listModels(String baseUrl, String apiKey) {
        String url = normalizeUrl(baseUrl) + "models";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        try {
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode data = response.getBody().path("data");
            if (!data.isArray()) {
                throw new IllegalStateException("Unexpected models API output structure");
            }

            List<String> models = new ArrayList<>();
            data.forEach(item -> {
                String id = item.path("id").asText("");
                if (!id.isBlank() && !models.contains(id)) {
                    models.add(id);
                }
            });
            return models;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "OpenAI-compatible provider call failed. operation=listModels", ex);
        }
    }

    private String normalizeUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private String providerCallFailure(String operation, String model, String mode) {
        return "OpenAI-compatible provider call failed. operation=" + operation
                + " model=" + AiStrings.safe(model)
                + " mode=" + AiStrings.safe(mode);
    }
}
