package com.interviewcoach.aiusage.service;

/**
 * 标准化后的单次 AI 调用 token 用量。
 */
public record AiUsageMetadata(
        int inputTokens,
        int outputTokens,
        int cacheCreationTokens,
        int cacheReadTokens,
        int reasoningTokens,
        String source,
        boolean estimated
) {
    public AiUsageMetadata {
        inputTokens = Math.max(0, inputTokens);
        outputTokens = Math.max(0, outputTokens);
        cacheCreationTokens = Math.max(0, cacheCreationTokens);
        cacheReadTokens = Math.max(0, cacheReadTokens);
        reasoningTokens = Math.max(0, reasoningTokens);
        source = source == null || source.isBlank() ? "unknown" : source;
    }

    public int totalTokens() {
        return inputTokens + outputTokens + cacheCreationTokens + cacheReadTokens + reasoningTokens;
    }
}
