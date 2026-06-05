package com.interviewcoach.common.api;

import java.util.List;

/**
 * 教练记忆 DTO，汇总 AI 在测评、训练、模拟面试中沉淀的结构化用户理解。
 *
 * @param id                   记忆记录 ID
 * @param targetId             关联的岗位目标 ID
 * @param sourceType           记忆来源类型：assessment / training / mockInterview
 * @param sourceId             来源记录 ID
 * @param observedStrengths    AI 观察到的用户优势
 * @param observedWeaknesses   AI 观察到的用户短板
 * @param recurringProblems    反复出现的问题
 * @param verifiedExperience   已确认的用户经历
 * @param unverifiedClaims     尚未确认的用户声明
 * @param recommendedNextFocus AI 推荐的下一步训练重点
 * @param avoidRepeating       不建议重复提及的内容
 * @param createdAt            创建时间
 */
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
