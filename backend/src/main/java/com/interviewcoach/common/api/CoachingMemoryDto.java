package com.interviewcoach.common.api;

import java.util.List;

public record CoachingMemoryDto(
        String id,
        String targetId,
        String sourceType,
        String sourceId,
        List<String> observedStrengths,
        List<String> observedWeaknesses,
        List<String> recurringProblems,
        List<String> verifiedExperience,
        List<String> unverifiedClaims,
        List<String> recommendedNextFocus,
        List<String> avoidRepeating,
        String createdAt
) {}
