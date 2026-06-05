package com.interviewcoach.common.api;

import java.util.List;

/**
 * 教练 Agent 状态 DTO，展示 Agent 的当前阶段、目标和最近决策。
 *
 * @param id                      Agent 实例 ID
 * @param targetId                关联的岗位目标 ID
 * @param status                  Agent 状态（运行中 / 已完成等）
 * @param currentStage            当前教练阶段（测评 / 训练 / 模拟面试等）
 * @param currentGoal             当前教练目标
 * @param activeFocusDimensions   当前关注的能力维度列表
 * @param nextRecommendedAction   AI 推荐的下一步动作
 * @param lastEventType           最近一次事件类型
 * @param lastDecisionSummary     最近一次决策摘要
 * @param lastRunAt               最近一次运行时间
 * @param createdAt               创建时间
 * @param updatedAt               更新时间
 */
public record CoachAgentDto(
        String id,
        String targetId,
        String status,
        String currentStage,
        String currentGoal,
        List<String> activeFocusDimensions,
        String nextRecommendedAction,
        String lastEventType,
        String lastDecisionSummary,
        String lastRunAt,
        String createdAt,
        String updatedAt
) {}
