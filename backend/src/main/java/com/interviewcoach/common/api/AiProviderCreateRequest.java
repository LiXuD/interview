package com.interviewcoach.common.api;

/**
 * 创建用户自定义 AI Provider 请求。
 *
 * @param name          Provider 显示名称
 * @param baseUrl       API 基础地址
 * @param apiKey        API 密钥（后端加密存储，不返回给客户端）
 * @param model         模型名称
 * @param openaiApiMode OpenAI API 模式（chatCompletions / responses）
 */
public record AiProviderCreateRequest(String name, String baseUrl, String apiKey, String model, String openaiApiMode) {
}
