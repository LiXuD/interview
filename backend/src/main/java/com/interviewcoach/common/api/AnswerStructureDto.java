package com.interviewcoach.common.api;

public record AnswerStructureDto(
        String background,
        String task,
        String action,
        String result,
        String tradeoff,
        String review
) {
}
