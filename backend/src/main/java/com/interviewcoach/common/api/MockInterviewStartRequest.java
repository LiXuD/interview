package com.interviewcoach.common.api;

/**
 * 模拟面试启动请求，指定目标岗位和可选的重点考察维度。
 *
 * @param targetId       岗位目标 ID
 * @param focusDimension 重点考察维度，可选
 */
public record MockInterviewStartRequest(
        String targetId,
        String focusDimension
) {
}
