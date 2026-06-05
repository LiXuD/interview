package com.interviewcoach.common.api;

/**
 * 训练任务 DTO，表示训练计划中的单个任务。
 *
 * @param id          任务 ID
 * @param title       任务标题
 * @param description 任务描述
 * @param status      状态：pending / in_progress / completed
 * @param feedback    AI 反馈（完成后填充）
 * @param completedAt 完成时间
 * @param dayIndex    所属天序号，从 0 开始
 */
public record TrainingTaskDto(
        String id,
        String title,
        String description,
        String status,
        String feedback,
        String completedAt,
        int dayIndex
) {
}
