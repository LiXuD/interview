package com.interviewcoach.common.api;

import java.util.List;

/**
 * 管理端 AI 用量全局概览 DTO。
 */
public record AdminAiUsageOverviewDto(
        long totalUsers,
        long activeUsers,
        long quotaExceededUsers,
        AiUsageSummaryDto summary,
        List<AiUsageDailyPointDto> daily,
        List<AiUsageBreakdownDto> topUsers,
        List<AiUsageBreakdownDto> topModels,
        List<AiUsageBreakdownDto> topTasks,
        List<AiUsageBreakdownDto> providers
) {}
