package com.interviewcoach.common.api;

import java.util.List;

/**
 * 自适应专项训练——单轮回答后的 AI 响应，由 AI 评分和决策，包含评分、反馈和下一题（若会话未结束）。
 *
 * @param action                 AI 决策的本轮动作（追问 / 换角度 / 达标停止等）
 * @param score                  本轮评分（0-100）
 * @param feedback               AI 对本轮回答的反馈
 * @param problems               AI 识别到的回答问题列表
 * @param nextQuestion           下一题内容（会话结束时为 null）
 * @param summary                会话结束时的总结（会话进行中为 null）
 * @param recommendedReviewPoints AI 推荐的复习要点列表
 */
public record AdaptiveTrainingTurnDto(
        String action,
        int score,
        String feedback,
        List<String> problems,
        String nextQuestion,
        String summary,
        List<String> recommendedReviewPoints
) {
}
