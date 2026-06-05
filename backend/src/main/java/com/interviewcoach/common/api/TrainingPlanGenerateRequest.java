package com.interviewcoach.common.api;

/**
 * 训练计划生成请求，指定要生成训练计划的岗位目标。
 *
 * @param targetId 岗位目标 ID
 */
public record TrainingPlanGenerateRequest(
        String targetId
) {
}
