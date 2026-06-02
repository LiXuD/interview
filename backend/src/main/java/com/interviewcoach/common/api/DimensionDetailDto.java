package com.interviewcoach.common.api;

import java.util.List;

public record DimensionDetailDto(
        String name,
        Integer latestScore,
        String trend,
        List<DimensionScoreEntry> scoreHistory,
        List<String> weaknesses,
        List<String> evidenceSources,
        List<String> nextFocus
) {
    public record DimensionScoreEntry(int score, String source, String createdAt) {
    }
}
