package com.interviewcoach.aiusage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * 解析 OpenAI-compatible 响应 usage，并提供低精度 fallback 估算。
 */
public final class AiUsageParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiUsageParser() {
    }

    public static Optional<AiUsageMetadata> fromOpenAiResponse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseJson);
            return fromOpenAiResponse(root);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static Optional<AiUsageMetadata> fromOpenAiResponse(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return Optional.empty();
        }
        JsonNode usage = root.path("usage");
        if (usage.isMissingNode() || usage.isNull()) {
            usage = root.path("response").path("usage");
        }
        if (usage.isMissingNode() || usage.isNull() || !usage.isObject()) {
            return Optional.empty();
        }
        int inputTokens = firstPositive(
                usage.path("input_tokens").asInt(0),
                usage.path("prompt_tokens").asInt(0));
        int outputTokens = firstPositive(
                usage.path("output_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0));
        int cacheReadTokens = firstPositive(
                usage.path("input_tokens_details").path("cached_tokens").asInt(0),
                usage.path("prompt_tokens_details").path("cached_tokens").asInt(0),
                usage.path("cache_read_input_tokens").asInt(0),
                usage.path("cache_read_tokens").asInt(0));
        int cacheCreationTokens = firstPositive(
                usage.path("cache_creation_input_tokens").asInt(0),
                usage.path("cache_creation_tokens").asInt(0),
                usage.path("input_tokens_details").path("cache_creation_tokens").asInt(0));
        int reasoningTokens = firstPositive(
                usage.path("output_tokens_details").path("reasoning_tokens").asInt(0),
                usage.path("completion_tokens_details").path("reasoning_tokens").asInt(0));
        return Optional.of(new AiUsageMetadata(
                inputTokens,
                outputTokens,
                cacheCreationTokens,
                cacheReadTokens,
                reasoningTokens,
                "openAiResponseUsage",
                false));
    }

    public static AiUsageMetadata estimate(String systemPrompt, String userPrompt, String response, String source) {
        int inputChars = length(systemPrompt) + length(userPrompt);
        int outputChars = length(response);
        int inputTokens = Math.max(1, inputChars / 4);
        int outputTokens = Math.max(0, outputChars / 4);
        return new AiUsageMetadata(inputTokens, outputTokens, 0, 0, 0, source, true);
    }

    private static int firstPositive(int... values) {
        for (int value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }
}
