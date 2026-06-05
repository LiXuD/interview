package com.interviewcoach.common.api;

import java.util.List;

/**
 * 教练进步追踪 Dashboard DTO，展示分数趋势、维度雷达和训练完成率。
 *
 * @param targetId              关联的岗位目标 ID
 * @param latestAssessmentScore 最新测评分数
 * @param scoreTrend            分数趋势历史
 * @param trainingCompletion    训练完成情况
 * @param dimensionSummary      各维度概要
 * @param recentWeaknesses      近期短板列表
 */
public record ProgressDashboardDto(
        String targetId,
        Integer latestAssessmentScore,
        List<ScoreTrendEntry> scoreTrend,
        TrainingCompletionDto trainingCompletion,
        List<DimensionSummaryDto> dimensionSummary,
        List<String> recentWeaknesses
) {
    /**
     * 分数趋势条目。
     *
     * @param score     分数
     * @param source    来源：assessment / mockInterview
     * @param createdAt 记录时间
     */
    public record ScoreTrendEntry(
            int score,
            String source,
            String createdAt
    ) {
    }

    /**
     * 训练完成情况统计。
     *
     * @param totalTasks     总任务数
     * @param completedTasks 已完成任务数
     * @param completionRate 完成率
     */
    public record TrainingCompletionDto(
            int totalTasks,
            int completedTasks,
            double completionRate
    ) {
    }

    /**
     * 维度概要，用于雷达图展示。
     *
     * @param name        维度名称
     * @param latestScore 最新分数
     * @param trend       趋势：rising / stable / declining
     */
    public record DimensionSummaryDto(
            String name,
            Integer latestScore,
            String trend
    ) {
    }
}
