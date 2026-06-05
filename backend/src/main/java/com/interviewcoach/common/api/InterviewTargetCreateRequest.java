package com.interviewcoach.common.api;

/**
 * 创建面试目标请求，包含岗位标题和 JD 原文。
 *
 * @param title 岗位标题
 * @param jd    JD 原文
 */
public record InterviewTargetCreateRequest(
        String title,
        String jd
) {
}
