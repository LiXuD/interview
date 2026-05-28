package com.interviewcoach.common.api;

import java.util.List;

public record AdaptiveTrainingTurnDto(
        String action,
        int score,
        String feedback,
        List<String> problems,
        String nextQuestion,
        String summary,
        List<String> recommendedReviewPoints
) {
}
