package com.interviewcoach.common.api;

/**
 * 管理端用户用量列表行 DTO。
 */
public record AdminAiUsageUserRowDto(
        String userId,
        String username,
        String email,
        String role,
        String createdAt,
        Long monthlyTokenQuota,
        long currentMonthTokens,
        Long remainingMonthlyTokens,
        boolean quotaExceeded,
        String lastUsedAt,
        AiUsageSummaryDto summary
) {}
