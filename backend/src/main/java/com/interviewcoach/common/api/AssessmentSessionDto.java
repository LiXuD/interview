package com.interviewcoach.common.api;

import java.util.List;

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
