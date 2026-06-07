package com.interviewcoach.common.api;

/**
 * 管理端更新用户月度 token 配额请求 DTO。null 表示不限制，0 表示禁止平台 AI。
 */
public record AdminTokenQuotaUpdateRequest(
        Long monthlyTokenQuota
) {}
