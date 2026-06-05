package com.interviewcoach.common.api;

import java.util.List;

/**
 * 教练 Agent 决策 DTO，记录 AI Agent 的当前目标、关注维度和下一步推荐动作。
 *
 * @param currentGoal            当前教练目标（如"评估系统设计能力"）
 * @param focusDimensions        本轮重点关注的能力维度列表
 * @param recommendedAction      AI 推荐的下一步动作
 * @param rationaleSummary       决策理由摘要
 * @param toolCalls              Agent 请求调用的工具列表
 * @param memoryUpdateRequired   是否需要更新教练记忆
 * @param planAdjustmentRequired 是否需要调整训练计划
 */
public record AgentDecisionDto(
        String currentGoal,
        List<String> focusDimensions,
        String recommendedAction,
        String rationaleSummary,
        List<AgentToolCallDto> toolCalls,
        boolean memoryUpdateRequired,
        boolean planAdjustmentRequired
) {}
