package com.interviewcoach.common.api;

/**
 * 技能图谱条目，由 AI 生成岗位画像时产出，描述某项技能的重要性、用户水平和差距。
 *
 * @param name       技能名称
 * @param importance 重要性：required / preferred / bonus
 * @param userLevel  用户当前水平
 * @param gap        与岗位要求的差距描述
 */
public record SkillMapItem(
        String name,
        String importance,
        String userLevel,
        String gap
) {
}
