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
        if (AiStrings.isBlank(properties.getBaseUrl()) || AiStrings.isBlank(properties.getApiKey())
                || AiStrings.isBlank(properties.getModel()) || AiStrings.isBlank(properties.getMode())) {
            throw new AiProviderCallFailedException(
                    "Platform AI configuration is incomplete. task=" + prompt.task()
                    + " provider=platformDefault model=" + AiStrings.safe(properties.getModel())
                    + " mode=" + AiStrings.safe(properties.getMode()) + ". "
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
                    "Platform AI call failed. task=" + prompt.task()
                    + " provider=platformDefault"
                    + " model=" + AiStrings.safe(properties.getModel())
                    + " mode=" + AiStrings.safe(properties.getMode()),
                    ex);
        }
    }
}
