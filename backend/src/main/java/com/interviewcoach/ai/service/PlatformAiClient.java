package com.interviewcoach.ai.service;

public interface PlatformAiClient {
    String generateJson(AiPrompt prompt);

    default <T> T generateEntity(AiPrompt prompt, Class<T> responseType) {
        return null;
    }
}
