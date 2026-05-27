package com.interviewcoach.common.api;

import java.util.List;

public record CoachingMemoryDto(
        String id,
        String targetId,
        String sourceType,
        String sourceId,
        List<CoachingMemoryItemDto> observedStrengths,
        List<CoachingMemoryItemDto> observedWeaknesses,
        List<CoachingMemoryItemDto> recurringProblems,
        List<CoachingMemoryItemDto> verifiedExperience,
        List<CoachingMemoryItemDto> unverifiedClaims,
        List<CoachingMemoryItemDto> recommendedNextFocus,
        List<CoachingMemoryItemDto> avoidRepeating,
        String createdAt
) {}
