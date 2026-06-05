package com.interviewcoach.common.api;

import java.util.List;

/**
 * 能力维度深度分析 DTO，由 AI 汇总指定岗位目标下各维度的趋势与短板。
 *
 * @param targetId   关联的岗位目标 ID
 * @param dimensions 各能力维度的详细分析
 */
public record DimensionAnalysisDto(
        String targetId,
        List<DimensionDetailDto> dimensions
) {
}
