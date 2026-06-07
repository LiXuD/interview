package com.interviewcoach.common.api;

/**
 * 管理端用户 token 配额响应 DTO。
 */
public record AdminTokenQuotaDto(
        String userId,
        Long monthlyTokenQuota,
        long currentMonthTokens,
        Long remainingMonthlyTokens,
        boolean quotaExceeded
) {}
