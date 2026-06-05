package com.interviewcoach.common.api;

/**
 * 自适应专项训练——提交回答请求。
 *
 * @param answer 用户对当前训练题的回答内容
 */
public record AdaptiveTrainingAnswerRequest(String answer) {
}
