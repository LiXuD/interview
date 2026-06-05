package com.interviewcoach.common.api;

import java.util.List;

/**
 * 测评结果 DTO，由 AI 基于测评回答生成，包含总分、各维度得分、优劣势和逐题评分。
 *
 * @param assessmentId   关联的测评会话 ID
 * @param totalScore     总分（0-100）
 * @param dimensions     各能力维度得分列表
 * @param strengths      优势项列表
 * @param weaknesses     短板项列表
 * @param nextActions    AI 推荐的下一步行动列表
 * @param questionScores 逐题评分详情列表
 */
public record AssessmentResultDto(String assessmentId, int totalScore, List<DimensionScore> dimensions, List<String> strengths, List<String> weaknesses, List<String> nextActions, List<AssessmentQuestionScoreDto> questionScores) {
}
