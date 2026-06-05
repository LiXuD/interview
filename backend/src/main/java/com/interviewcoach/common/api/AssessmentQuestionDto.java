package com.interviewcoach.common.api;

import java.util.List;

/**
 * 测评题目 DTO，由 AI 基于岗位画像生成，包含题目内容、所属维度、难度和评分标准。
 *
 * @param question   题目内容
 * @param dimension  所属能力维度（对应 AssessmentDimensionName）
 * @param difficulty 难度等级
 * @param intent     题目考察意图
 * @param rubric     评分标准要点列表
 */
public record AssessmentQuestionDto(
        String question,
        String dimension,
        String difficulty,
        String intent,
        List<String> rubric
) {
}
