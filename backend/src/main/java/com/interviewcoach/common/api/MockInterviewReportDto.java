package com.interviewcoach.common.api;

import java.util.List;

public record MockInterviewReportDto(String mockInterviewId, int overallScore, List<DimensionScore> dimensionScores, String summary, List<String> strengths, List<String> weaknesses, List<String> improvedAnswers, List<String> likelyFollowUpPoints, List<String> nextTrainingTasks) {
}
