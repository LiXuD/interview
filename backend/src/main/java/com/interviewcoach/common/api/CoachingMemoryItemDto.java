package com.interviewcoach.common.api;

/**
 * 教练记忆条目 DTO，包含具体内容、来源和可信度。
 *
 * @param content    记忆内容
 * @param source     来源：assessment / training / mockInterview / userCorrection
 * @param confidence 可信度：confirmed / observed / corrected / inferred / rejected
 */
public record CoachingMemoryItemDto(
        String content,
        String source,
        String confidence
) {}
