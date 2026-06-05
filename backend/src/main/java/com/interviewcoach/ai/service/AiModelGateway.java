package com.interviewcoach.ai.service;

/**
 * AI 模型调用网关接口。封装对平台 AI 和用户自定义 Provider 的统一调用入口，
 * 支持返回原始 JSON 字符串或直接映射为强类型实体。
 */
public interface AiModelGateway {

    /**
     * 调用 AI 模型并返回原始 JSON 字符串。
     *
     * @param prompt AI 调用请求，包含 task、systemPrompt 和 userPrompt
     * @return AI 返回的 JSON 字符串
     */
    String generateJson(AiPrompt prompt);

    /**
     * 调用 AI 模型并直接将响应映射为指定类型的实体。
     *
     * @param prompt     AI 调用请求
     * @param responseType 目标实体类型
     * @return 映射后的实体，或 null（当 Spring AI 未启用时）
     */
    <T> T generateEntity(AiPrompt prompt, Class<T> responseType);
}
