package com.interviewcoach.common.api;

import java.util.List;

public record TrainingFeedbackDto(String taskId, int score, String feedback, List<String> problems, String rewrittenAnswer, String followUpQuestion, List<String> recommendedReviewPoints) {
}
