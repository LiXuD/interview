package com.interviewcoach.common.api;

/**
 * 训练任务回答请求，用户对训练题目的回答。
 *
 * @param answer 用户的回答内容
 */
public record TrainingTaskAnswerRequest(
        String answer
) {
}
