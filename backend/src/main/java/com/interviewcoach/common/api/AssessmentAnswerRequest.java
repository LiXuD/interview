package com.interviewcoach.common.api;

/**
 * 测评——提交单题回答请求。
 *
 * @param answer 用户对当前测评题的回答内容
 */
public record AssessmentAnswerRequest(String answer) {
}
