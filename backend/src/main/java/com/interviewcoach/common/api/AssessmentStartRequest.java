package com.interviewcoach.common.api;

/**
 * 开始测评请求。
 *
 * @param targetId 要测评的岗位目标 ID
 */
public record AssessmentStartRequest(String targetId) {
}
