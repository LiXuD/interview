package com.interviewcoach.common.api;

public record CoachingMemoryItemDto(
        String content,
        String source,
        String confidence
) {}
