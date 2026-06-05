package com.interviewcoach.common.api;

/**
 * 更新面试目标请求，所有字段均为可选，仅更新非 null 字段。
 *
 * @param title  岗位标题
 * @param jd     JD 原文
 * @param status 状态：active / archived
 */
public record InterviewTargetUpdateRequest(
        String title,
        String jd,
        String status
) {
}
