package com.interviewcoach.common.api;

import java.util.List;

/**
 * 训练任务反馈 DTO，由 AI 基于用户训练回答生成，包含评分、诊断和改进建议。
 *
 * @param taskId                关联的训练任务 ID
 * @param score                 评分，0-100
 * @param feedback              整体反馈
 * @param problems              AI 诊断出的问题列表
 * @param rewrittenAnswer       AI 改写后的示范回答
 * @param followUpQuestion      AI 提出的追问
 * @param recommendedReviewPoints 推荐复习的知识点
 */
public record TrainingFeedbackDto(
        String taskId,
        int score,
        String feedback,
        List<String> problems,
        String rewrittenAnswer,
        String followUpQuestion,
        List<String> recommendedReviewPoints
) {
}
