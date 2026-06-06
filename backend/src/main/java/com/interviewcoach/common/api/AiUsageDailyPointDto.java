package com.interviewcoach.common.api;

/**
 * 单日 AI token 用量聚合点。
 */
public record AiUsageDailyPointDto(
        String date,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        long totalInputTokens,
        long totalOutputTokens,
        long totalCacheCreationTokens,
        long totalCacheReadTokens,
        long totalReasoningTokens,
        long totalTokens
) {
}
