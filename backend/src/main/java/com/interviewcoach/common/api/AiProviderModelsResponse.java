package com.interviewcoach.common.api;

import java.util.List;

/**
 * AI Provider 可用模型列表响应。
 *
 * @param models 该 Provider 支持的模型名称列表
 */
public record AiProviderModelsResponse(List<String> models) {
}
