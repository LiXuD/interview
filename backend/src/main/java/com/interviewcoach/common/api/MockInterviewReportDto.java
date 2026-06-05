package com.interviewcoach.common.api;

import java.util.List;

/**
 * 模拟面试报告 DTO，由 AI 基于模拟面试表现生成，包含总分、各维度评分、优劣势分析和改进建议。
 *
 * @param mockInterviewId    关联的模拟面试 ID
 * @param overallScore       总分，0-100
 * @param dimensionScores    各维度评分
 * @param summary            整体总结
 * @param strengths          优势列表
 * @param weaknesses         短板列表
 * @param improvedAnswers    改进后的示范回答
 * @param likelyFollowUpPoints AI 预判的后续追问点
 * @param nextTrainingTasks  推荐的下一步训练任务
 */
public record MockInterviewReportDto(
        String mockInterviewId,
        int overallScore,
        List<DimensionScore> dimensionScores,
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> improvedAnswers,
        List<String> likelyFollowUpPoints,
        List<String> nextTrainingTasks
) {
}
