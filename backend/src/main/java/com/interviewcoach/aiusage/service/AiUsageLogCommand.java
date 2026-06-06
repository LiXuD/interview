package com.interviewcoach.aiusage.service;

import java.util.UUID;

/**
 * 单次 AI 调用 usage 记录命令。只包含低风险元数据和 token 数字。
 */
public record AiUsageLogCommand(
        UUID userId,
        UUID targetId,
        String requestId,
        String task,
        String providerType,
        UUID providerId,
        String model,
        String mode,
        String usageSource,
        int inputTokens,
        int outputTokens,
        int cacheCreationTokens,
        int cacheReadTokens,
        int reasoningTokens,
        int totalTokens,
        boolean estimated,
        boolean success,
        boolean parseFailed,
        boolean validationFailed,
        boolean timeout,
        int retryCount,
        long durationMs
) {
}
