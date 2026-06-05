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

/**
 * OpenAI-compatible API 客户端。通过 RestTemplate 直接调用兼容 OpenAI 的 API，
 * 支持 chatCompletions 和 responses 两种模式，以及连接测试和模型列表查询。
 */
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

    /**
     * 调用 AI 模型并返回 JSON 字符串。根据 openaiApiMode 选择调用路径。
     *
     * @param baseUrl       API base URL
     * @param apiKey        API Key
     * @param model         模型名称
     * @param openaiApiMode API 模式：chatCompletions 或 responses
     * @param systemPrompt  系统提示词
     * @param userPrompt    用户提示词
     * @return AI 返回的 JSON 字符串
     */
    public String generateJson(String baseUrl, String apiKey, String model,
                               String openaiApiMode, String systemPrompt, String userPrompt) {
        if (MODE_RESPONSES.equals(openaiApiMode)) {
            return callResponsesApi(baseUrl, apiKey, model, systemPrompt, userPrompt);
        } else {
            return callChatCompletionsApi(baseUrl, apiKey, model, systemPrompt, userPrompt);
        }
    }

    /** 调用 chatCompletions 模式 API */
    private String callChatCompletionsApi(String baseUrl, String apiKey, String model,
                                          String systemPrompt, String userPrompt) {
        // 1. 构建请求 URL 和 Headers
        String url = normalizeUrl(baseUrl) + "chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 2. 构建请求体：包含 model、messages 和 JSON 响应格式
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        try {
            // 3. 发送请求并从响应中提取 content 字段
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            return response.getBody().path("choices").get(0).path("message").path("content").asText();
        } catch (Exception ex) {
            throw new IllegalStateException(providerCallFailure("chatCompletions", model, MODE_CHAT_COMPLETIONS), ex);
        }
    }

    /** 调用 responses 模式 API */
    private String callResponsesApi(String baseUrl, String apiKey, String model,
                                    String systemPrompt, String userPrompt) {
        // 1. 构建请求 URL 和 Headers
        String url = normalizeUrl(baseUrl) + "responses";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 2. 构建请求体：将 system 和 user prompt 合并为 input
        Map<String, Object> body = Map.of(
                "model", model,
                "input", systemPrompt + "\n\n" + userPrompt
        );

        try {
            // 3. 发送请求并从 output[0].content[0].text 中提取结果
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

    /**
     * 测试 Provider 连接是否可用
     *
     * @param baseUrl       API base URL
     * @param apiKey        API Key
     * @param model         模型名称
     * @param openaiApiMode API 模式
     * @throws IllegalStateException 连接测试失败时
     */
    public void testConnection(String baseUrl, String apiKey, String model, String openaiApiMode) {
        generateJson(baseUrl, apiKey, model, openaiApiMode,
                "Reply with valid JSON: {\"ok\":true}",
                "Say ok.");
    }

    /**
     * 获取 Provider 可用的模型列表
     *
     * @param baseUrl API base URL
     * @param apiKey  API Key
     * @return 模型 ID 列表
     * @throws IllegalStateException API 调用失败或响应结构异常时
     */
    public List<String> listModels(String baseUrl, String apiKey) {
        // 1. 构建请求 URL 和认证 Headers
        String url = normalizeUrl(baseUrl) + "models";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        try {
            // 2. 发送 GET 请求获取模型列表
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode data = response.getBody().path("data");
            if (!data.isArray()) {
                throw new IllegalStateException("Unexpected models API output structure");
            }

            // 3. 提取去重后的模型 ID 列表
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
