package com.interviewcoach.common.api;

import java.util.List;

/**
 * 训练计划 DTO，由 AI 基于测评短板生成的结构化训练方案。
 *
 * @param id        训练计划 ID
 * @param targetId  关联的岗位目标 ID
 * @param tasks     训练任务列表
 * @param totalDays 计划总天数
 * @param status    状态：active / completed
 * @param createdAt 创建时间
 */
public record TrainingPlanDto(
        String id,
        String targetId,
        List<TrainingTaskDto> tasks,
        int totalDays,
        String status,
        String createdAt
) {
}
