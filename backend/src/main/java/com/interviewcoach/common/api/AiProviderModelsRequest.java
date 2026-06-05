package com.interviewcoach.common.api;

/**
 * 查询 AI Provider 可用模型列表请求。
 *
 * @param baseUrl Provider 的 API 基础地址
 * @param apiKey  API 密钥（用于验证连接并获取模型列表）
 */
public record AiProviderModelsRequest(String baseUrl, String apiKey) {
}
