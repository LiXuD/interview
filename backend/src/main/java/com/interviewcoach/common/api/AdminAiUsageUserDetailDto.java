package com.interviewcoach.common.api;

import java.util.List;

/**
 * 管理端单用户用量详情 DTO。
 */
public record AdminAiUsageUserDetailDto(
        String userId,
        String username,
        String email,
        String role,
        String createdAt,
        Long monthlyTokenQuota,
        long currentMonthTokens,
        Long remainingMonthlyTokens,
        boolean quotaExceeded,
        AiUsageSummaryDto summary,
        List<AiUsageDailyPointDto> daily,
        List<AiUsageBreakdownDto> byTask,
        List<AiUsageBreakdownDto> byModel,
        List<AiUsageBreakdownDto> byProvider
) {}
