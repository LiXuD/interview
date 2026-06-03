package com.interviewcoach.common.api;

import java.util.List;

public record CoachAgentDto(
        String id,
        String targetId,
        String status,
        String currentStage,
        String currentGoal,
        List<String> activeFocusDimensions,
        String nextRecommendedAction,
        String lastEventType,
        String lastDecisionSummary,
        String lastRunAt,
        String createdAt,
        String updatedAt
) {}
