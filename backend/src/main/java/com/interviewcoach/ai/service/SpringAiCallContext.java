package com.interviewcoach.ai.service;

import com.interviewcoach.ai.entity.AiProvider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring AI 调用上下文工厂。为 Spring AI 的 advisor 机制提供标准化的元数据 Map，
 * 包含 task、targetId、requestId、provider、model、mode 等低风险可观测字段。
 */
public final class SpringAiCallContext {

    private SpringAiCallContext() {
    }

    /**
     * 构建用户自定义 Provider 的调用上下文。
     *
     * @param prompt    AI 调用请求
     * @param provider  用户 Provider 实体
     * @param requestId 请求 ID
     * @return 包含 provider、model、mode 等元数据的上下文 Map
     */
    public static Map<String, Object> user(AiPrompt prompt, AiProvider provider, String requestId) {
        Map<String, Object> context = base(prompt, requestId);
        context.put("ai.provider", "userOpenAICompatible");
        context.put("ai.providerId", provider.getId() == null ? "unknown" : provider.getId().toString());
        context.put("ai.model", AiStrings.safe(provider.getModel()));
        context.put("ai.mode", AiStrings.safe(provider.getOpenaiApiMode()));
        return context;
    }

    /**
     * 构建平台默认 AI 的调用上下文。
     *
     * @param prompt     AI 调用请求
     * @param properties 平台 AI 配置属性
     * @param requestId  请求 ID
     * @return 包含 provider、model、mode 等元数据的上下文 Map
     */
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
