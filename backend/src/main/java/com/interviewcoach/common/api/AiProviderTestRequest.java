package com.interviewcoach.common.api;

/**
 * 测试 AI Provider 连接请求。
 *
 * @param baseUrl       Provider 的 API 基础地址
 * @param apiKey        API 密钥
 * @param model         要测试的模型名称
 * @param openaiApiMode OpenAI API 模式（chatCompletions / responses）
 */
public record AiProviderTestRequest(String baseUrl, String apiKey, String model, String openaiApiMode) {
}
