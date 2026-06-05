package com.interviewcoach.common.api;

/**
 * 模拟面试会话 DTO，表示一次进行中或已完成的模拟面试。
 *
 * @param id                会话 ID
 * @param targetId          关联的岗位目标 ID
 * @param status            状态：in_progress / finished
 * @param currentQuestion   当前面试问题
 * @param conversationTurns 已进行的对话轮数
 * @param focusDimension    重点考察维度
 */
public record MockInterviewSessionDto(
        String id,
        String targetId,
        String status,
        String currentQuestion,
        int conversationTurns,
        String focusDimension
) {
}
