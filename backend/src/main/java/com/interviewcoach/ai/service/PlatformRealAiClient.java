package com.interviewcoach.ai.service;

import com.interviewcoach.common.error.AiProviderCallFailedException;

/**
 * 平台真实 AI 客户端（旧版直调路径）。通过 {@link OpenAiCompatibleClient} 调用平台配置的
 * OpenAI-compatible API。在 Spring AI 未启用时作为主客户端使用。
 */
public class PlatformRealAiClient implements PlatformAiClient {

    private final OpenAiCompatibleClient openAiClient;
    private final PlatformAiProperties properties;

    public PlatformRealAiClient(OpenAiCompatibleClient openAiClient, PlatformAiProperties properties) {
        this.openAiClient = openAiClient;
        this.properties = properties;
    }

    /**
     * 调用平台 AI 生成 JSON 响应。配置不完整时直接抛出异常，不静默回退。
     *
     * @param prompt AI 调用请求
     * @return AI 返回的 JSON 字符串
     * @throws AiProviderCallFailedException 配置不完整或调用失败时
     */
    @Override
    public String generateJson(AiPrompt prompt) {
        // 1. 校验平台 AI 配置完整性
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
            // 2. 委托 OpenAI-compatible 客户端调用
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
