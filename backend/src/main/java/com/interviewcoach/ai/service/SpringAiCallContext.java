package com.interviewcoach.ai.service;

import com.interviewcoach.ai.entity.AiProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SpringAiCallContext {

    private SpringAiCallContext() {
    }

    public static Map<String, Object> user(AiPrompt prompt, AiProvider provider, String requestId) {
        Map<String, Object> context = base(prompt, requestId);
        context.put("ai.provider", "userOpenAICompatible");
        context.put("ai.providerId", provider.getId() == null ? "unknown" : provider.getId().toString());
        context.put("ai.model", AiStrings.safe(provider.getModel()));
        context.put("ai.mode", AiStrings.safe(provider.getOpenaiApiMode()));
        return context;
    }

    public static Map<String, Object> platform(AiPrompt prompt, PlatformAiProperties properties, String requestId) {
        Map<String, Object> context = base(prompt, requestId);
        context.put("ai.provider", "platformDefault");
        context.put("ai.model", AiStrings.safe(properties.getModel()));
        context.put("ai.mode", AiStrings.safe(properties.getMode()));
        return context;
    }

    private static Map<String, Object> base(AiPrompt prompt, String requestId) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("ai.task", AiStrings.safe(prompt.task()));
        context.put("ai.targetId", AiStrings.safe(prompt.targetId()));
        context.put("ai.requestId", AiStrings.safe(requestId));
        return context;
    }
}
