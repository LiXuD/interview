package com.interviewcoach.ai.service;

import com.interviewcoach.common.error.AiProviderCallFailedException;

public class PlatformRealAiClient implements PlatformAiClient {

    private final OpenAiCompatibleClient openAiClient;
    private final PlatformAiProperties properties;

    public PlatformRealAiClient(OpenAiCompatibleClient openAiClient, PlatformAiProperties properties) {
        this.openAiClient = openAiClient;
        this.properties = properties;
    }

    @Override
    public String generateJson(AiPrompt prompt) {
        if (isBlank(properties.getBaseUrl()) || isBlank(properties.getApiKey())
                || isBlank(properties.getModel()) || isBlank(properties.getMode())) {
            throw new AiProviderCallFailedException(
                    "Platform AI configuration is incomplete. "
                    + "Required: IC_PLATFORM_AI_BASE_URL, IC_PLATFORM_AI_API_KEY, IC_PLATFORM_AI_MODEL, IC_PLATFORM_AI_MODE",
                    null);
        }
        try {
            return openAiClient.generateJson(
                    properties.getBaseUrl(),
                    properties.getApiKey(),
                    properties.getModel(),
                    properties.getMode(),
                    prompt.systemPrompt(),
                    prompt.userPrompt());
        } catch (Exception ex) {
            throw new AiProviderCallFailedException(
                    "Platform AI call failed: " + ex.getMessage(), ex);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
