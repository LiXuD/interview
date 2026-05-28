package com.interviewcoach.common.api;

import java.util.List;

public record AdaptiveTrainingRoundDto(
        int roundIndex,
        String question,
        String answer,
        String action,
        int score,
        String feedback,
        List<String> problems
) {
}
