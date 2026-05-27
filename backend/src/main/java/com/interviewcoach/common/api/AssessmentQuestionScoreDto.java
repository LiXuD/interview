package com.interviewcoach.common.api;

import java.util.List;

public record AssessmentQuestionScoreDto(
        int questionIndex,
        int score,
        String dimension,
        String feedback,
        List<String> problems,
        String improvedExample
) {
}
