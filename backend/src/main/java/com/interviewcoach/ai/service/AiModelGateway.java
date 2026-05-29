package com.interviewcoach.ai.service;

public interface AiModelGateway {

    String generateJson(AiPrompt prompt);

    <T> T generateEntity(AiPrompt prompt, Class<T> responseType);
}
