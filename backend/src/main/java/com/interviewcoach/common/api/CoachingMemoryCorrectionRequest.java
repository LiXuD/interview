package com.interviewcoach.common.api;

/**
 * 教练记忆纠错请求，用户对 AI 记忆的某条内容进行纠正。
 *
 * @param field     要纠正的字段名（如 skills、projects 等）
 * @param itemIndex 要纠正的条目在列表中的索引
 * @param source    纠正来源（用户主动纠正）
 * @param content   用户纠正后的内容
 */
public record CoachingMemoryCorrectionRequest(
        String field,
        int itemIndex,
        String source,
        String content
) {}
