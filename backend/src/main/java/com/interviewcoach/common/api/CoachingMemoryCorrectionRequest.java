package com.interviewcoach.common.api;

public record CoachingMemoryCorrectionRequest(
        String field,
        int itemIndex,
        String source,
        String content
) {}
