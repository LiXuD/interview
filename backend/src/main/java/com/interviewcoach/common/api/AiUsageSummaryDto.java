package com.interviewcoach.common.api;

/**
 * 当前用户 AI token 用量汇总。
 */
public record AiUsageSummaryDto(
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
