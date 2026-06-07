package com.interviewcoach.aiusage.service;

import com.interviewcoach.aiusage.entity.AiUsageLog;
import com.interviewcoach.common.api.AiUsageBreakdownDto;
import com.interviewcoach.common.api.AiUsageDailyPointDto;
import com.interviewcoach.common.api.AiUsageSummaryDto;

/**
 * AI 用量日志累加器，用于将多条日志聚合为汇总、逐日或分解 DTO。
 */
public final class UsageAccumulator {

    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private long estimatedRequests;
    private long totalInputTokens;
    private long totalOutputTokens;
    private long totalCacheCreationTokens;
    private long totalCacheReadTokens;
    private long totalReasoningTokens;
    private long totalTokens;

    public void add(AiUsageLog log) {
        totalRequests++;
        if (log.isSuccess()) {
            successfulRequests++;
        } else {
            failedRequests++;
        }
        if (log.isEstimated()) {
            estimatedRequests++;
        }
        totalInputTokens += log.getInputTokens();
        totalOutputTokens += log.getOutputTokens();
        totalCacheCreationTokens += log.getCacheCreationTokens();
        totalCacheReadTokens += log.getCacheReadTokens();
        totalReasoningTokens += log.getReasoningTokens();
        totalTokens += log.getTotalTokens();
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public AiUsageSummaryDto toSummary() {
        return new AiUsageSummaryDto(
                totalRequests, successfulRequests, failedRequests, estimatedRequests,
                totalInputTokens, totalOutputTokens, totalCacheCreationTokens,
                totalCacheReadTokens, totalReasoningTokens, totalTokens);
    }

    public AiUsageDailyPointDto toDaily(String date) {
        return new AiUsageDailyPointDto(
                date, totalRequests, successfulRequests, failedRequests,
                totalInputTokens, totalOutputTokens, totalCacheCreationTokens,
                totalCacheReadTokens, totalReasoningTokens, totalTokens);
    }

    public AiUsageBreakdownDto toBreakdown(String name) {
        return new AiUsageBreakdownDto(
                name, totalRequests, successfulRequests, failedRequests, estimatedRequests,
                totalInputTokens, totalOutputTokens, totalCacheCreationTokens,
                totalCacheReadTokens, totalReasoningTokens, totalTokens);
    }
}
