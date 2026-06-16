package com.interviewcoach.common.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建面试目标请求，包含岗位标题和 JD 原文。
 *
 * @param title 岗位标题
 * @param jd    JD 原文
 */
public record InterviewTargetCreateRequest(
        @NotBlank(message = "title must not be blank")
        String title,
        @NotBlank(message = "jd must not be blank")
        String jd
) {
}
