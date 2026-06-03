package com.interviewcoach.common.api;

import java.util.List;

public record AgentDecisionDto(
        String currentGoal,
        List<String> focusDimensions,
        String recommendedAction,
        String rationaleSummary,
        List<AgentToolCallDto> toolCalls,
        boolean memoryUpdateRequired,
        boolean planAdjustmentRequired
) {}
