package com.interviewcoach.common.api;

import java.util.List;

/**
 * 单个能力维度的详细分析，由 AI 生成，包含最新分数、趋势、历史记录和改进方向。
 *
 * @param name           维度名称
 * @param latestScore    最新分数
 * @param trend          趋势：rising / stable / declining
 * @param scoreHistory   分数历史记录
 * @param weaknesses     已识别的短板
 * @param evidenceSources 证据来源列表
 * @param nextFocus      推荐的下一步训练重点
 */
public record DimensionDetailDto(
        String name,
        Integer latestScore,
        String trend,
        List<DimensionScoreEntry> scoreHistory,
        List<String> weaknesses,
        List<String> evidenceSources,
        List<String> nextFocus
) {
    /**
     * 维度分数历史条目。
     *
     * @param score     分数
     * @param source    来源：assessment / mockInterview
     * @param createdAt 记录时间
     */
    public record DimensionScoreEntry(
            int score,
            String source,
            String createdAt
    ) {
    }
}
