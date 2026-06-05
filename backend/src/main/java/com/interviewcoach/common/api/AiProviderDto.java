package com.interviewcoach.common.api;

/**
 * AI Provider 配置 DTO（返回给客户端，不包含 apiKey）。
 *
 * @param id            Provider ID
 * @param name          Provider 显示名称
 * @param baseUrl       API 基础地址
 * @param model         模型名称
 * @param openaiApiMode OpenAI API 模式（chatCompletions / responses）
 * @param isDefault     是否为当前用户的默认 Provider
 * @param createdAt     创建时间
 */
public record AiProviderDto(String id, String name, String baseUrl, String model, String openaiApiMode, boolean isDefault, String createdAt) {
}
