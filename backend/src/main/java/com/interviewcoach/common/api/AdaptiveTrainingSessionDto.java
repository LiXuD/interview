package com.interviewcoach.common.api;

import java.util.List;

/**
 * 自适应专项训练——会话状态 DTO，返回给 iOS 端用于展示训练进度和当前状态。
 *
 * @param id              会话 ID
 * @param taskId          关联的训练任务 ID
 * @param status          会话状态（进行中 / 已完成等）
 * @param roundIndex      当前轮次序号
 * @param minRounds       最少训练轮次
 * @param maxRounds       最多训练轮次
 * @param currentQuestion 当前待回答的题目
 * @param lastAction      AI 上一轮的决策动作
 * @param summary         会话完成后的总结（会话结束时才有值）
 * @param rounds          已完成的所有轮次记录
 */
public record AdaptiveTrainingSessionDto(
        String id,
        String taskId,
        String status,
        int roundIndex,
        int minRounds,
        int maxRounds,
        String currentQuestion,
        String lastAction,
        String summary,
        List<AdaptiveTrainingRoundDto> rounds
) {
}
