package com.interviewcoach.common.api;

import java.util.List;

/**
 * 测评会话 DTO，返回给 iOS 端用于展示测评进度和题目。
 *
 * @param id             测评会话 ID
 * @param targetId       关联的岗位目标 ID
 * @param status         会话状态（进行中 / 已完成等）
 * @param questionIndex  当前题目序号
 * @param totalQuestions  总题数（固定 5 题）
 * @param currentQuestion 当前待回答的题目
 * @param questions       全部题目列表（测评完成后返回）
 * @param questionScores  逐题评分列表（测评完成后返回）
 */
public record AssessmentSessionDto(
        String id,
        String targetId,
        String status,
        int questionIndex,
        int totalQuestions,
        AssessmentQuestionDto currentQuestion,
        List<AssessmentQuestionDto> questions,
        List<AssessmentQuestionScoreDto> questionScores
) {
}
