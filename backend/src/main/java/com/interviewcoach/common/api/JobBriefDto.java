package com.interviewcoach.common.api;

import java.util.List;

/**
 * 岗位画像 DTO，由 AI 基于 JD 和候选人经历生成的结构化岗位分析。
 *
 * @param targetId        关联的岗位目标 ID
 * @param roleSummary     岗位角色概述
 * @param skillMap        技能图谱
 * @param mustHaveSkills  必备技能列表
 * @param niceToHaveSkills 加分技能列表
 * @param businessContext  业务背景信息
 * @param interviewTopics  面试考察主题
 * @param candidateMatch  候选人匹配点
 * @param riskAreas       候选人风险领域
 * @param confidence      AI 分析置信度，0-1
 */
public record JobBriefDto(
        String targetId,
        String roleSummary,
        List<SkillMapItem> skillMap,
        List<String> mustHaveSkills,
        List<String> niceToHaveSkills,
        List<String> businessContext,
        List<String> interviewTopics,
        List<String> candidateMatch,
        List<String> riskAreas,
        double confidence
) {
}
