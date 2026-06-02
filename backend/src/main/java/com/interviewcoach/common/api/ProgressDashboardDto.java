package com.interviewcoach.common.api;

import java.util.List;

public record ProgressDashboardDto(
        String targetId,
        Integer latestAssessmentScore,
        List<ScoreTrendEntry> scoreTrend,
        TrainingCompletionDto trainingCompletion,
        List<DimensionSummaryDto> dimensionSummary,
        List<String> recentWeaknesses
) {
    public record ScoreTrendEntry(int score, String source, String createdAt) {
    }

    public record TrainingCompletionDto(int totalTasks, int completedTasks, double completionRate) {
    }

    public record DimensionSummaryDto(String name, Integer latestScore, String trend) {
    }
}
