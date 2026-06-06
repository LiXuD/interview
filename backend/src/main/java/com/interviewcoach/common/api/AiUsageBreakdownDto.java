package com.interviewcoach.common.api;

/**
 * AI token 用量按任务、模型或 Provider 的聚合项。
 */
public record AiUsageBreakdownDto(
        String name,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        long estimatedRequests,
        long totalInputTokens,
        long totalOutputTokens,
        long totalCacheCreationTokens,
        long totalCacheReadTokens,
        long totalReasoningTokens,
        long totalTokens
) {
}
