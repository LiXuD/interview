package com.interviewcoach.common.api;

import java.util.List;

public record AssessmentQuestionDto(
        String question,
        String dimension,
        String difficulty,
        String intent,
        List<String> rubric
) {
}
