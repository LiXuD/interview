package com.interviewcoach.common.api;

/**
 * 教练记忆条目 DTO，包含具体内容、来源和可信度。
 *
 * @param content    记忆内容
 * @param source     来源可信语义：confirmed / observed / corrected / inferred / rejected
 * @param confidence 可信度：high / medium / low
 */
public record CoachingMemoryItemDto(
        String content,
        String source,
        String confidence
) {}
