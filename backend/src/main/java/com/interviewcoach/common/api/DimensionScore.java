package com.interviewcoach.common.api;

/**
 * 维度评分，用于测评结果和模拟面试报告等场景的单维度打分。
 *
 * @param name   维度名称
 * @param score  分数，0-100
 * @param reason 评分理由
 */
public record DimensionScore(
        String name,
        int score,
        String reason
) {
}
