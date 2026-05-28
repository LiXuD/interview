package com.interviewcoach.common.api;

import java.util.List;

public record AssessmentResultDto(String assessmentId, int totalScore, List<DimensionScore> dimensions, List<String> strengths, List<String> weaknesses, List<String> nextActions, List<AssessmentQuestionScoreDto> questionScores) {
}
