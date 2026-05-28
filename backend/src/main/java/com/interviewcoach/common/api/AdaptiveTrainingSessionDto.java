package com.interviewcoach.common.api;

import java.util.List;

public record AdaptiveTrainingSessionDto(
        String id,
        String taskId,
        String status,
        int roundIndex,
        int minRounds,
        int maxRounds,
        String currentQuestion,
        String lastAction,
        String summary,
        List<AdaptiveTrainingRoundDto> rounds
) {
}
