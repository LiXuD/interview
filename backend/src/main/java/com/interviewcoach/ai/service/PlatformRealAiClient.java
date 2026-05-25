package com.interviewcoach.ai.service;

public class PlatformRealAiClient implements PlatformAiClient {

    private final OpenAiCompatibleClient openAiClient;
    private final PlatformAiProperties properties;

    public PlatformRealAiClient(OpenAiCompatibleClient openAiClient, PlatformAiProperties properties) {
        this.openAiClient = openAiClient;
        if (isBlank(properties.getBaseUrl()) || isBlank(properties.getApiKey())
                || isBlank(properties.getModel()) || isBlank(properties.getMode())) {
            throw new IllegalStateException(
                    "Platform AI is enabled but configuration is incomplete. "
                    + "Required: app.ai.platform.base-url, api-key, model, mode");
        }
        this.properties = properties;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public String generateJson(AiPrompt prompt) {
        return openAiClient.generateJson(
                properties.getBaseUrl(),
                properties.getApiKey(),
                properties.getModel(),
                properties.getMode(),
                prompt.systemPrompt(),
                prompt.userPrompt()
        );
    }
}
