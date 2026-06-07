package com.interviewcoach.common.api;

import java.util.List;

/**
 * 管理端用户用量分页 DTO。
 */
public record AdminAiUsageUsersPageDto(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<AdminAiUsageUserRowDto> items
) {}
