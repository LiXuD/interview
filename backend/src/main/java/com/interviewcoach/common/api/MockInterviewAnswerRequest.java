package com.interviewcoach.common.api;

/**
 * 模拟面试回答请求，用户对当前面试问题的回答。
 *
 * @param answer 用户的回答内容
 */
public record MockInterviewAnswerRequest(
        String answer
) {
}
