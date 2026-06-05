package com.interviewcoach.ai.service;

/**
 * 平台 AI 客户端接口。定义平台级 AI 调用的统一抽象，
 * 支持返回 JSON 字符串和结构化实体两种调用方式。
 * <p>实现包括：{@link LocalPlatformAiClient}（stub）、{@link PlatformRealAiClient}（旧版直调）、
 * {@link SpringAiPlatformClient}（Spring AI 底座）。</p>
 */
public interface PlatformAiClient {

    /**
     * 调用 AI 并返回原始 JSON 字符串。
     *
     * @param prompt AI 调用请求
     * @return AI 返回的 JSON 字符串
     */
    String generateJson(AiPrompt prompt);

    /**
     * 调用 AI 并将响应直接映射为指定类型的实体。
     * 默认返回 null，由支持结构化输出的实现覆盖。
     */
    default <T> T generateEntity(AiPrompt prompt, Class<T> responseType) {
        return null;
    }
}
