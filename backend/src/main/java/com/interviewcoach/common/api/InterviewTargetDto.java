package com.interviewcoach.common.api;

/**
 * 面试目标 DTO，表示用户创建的一个目标岗位。
 *
 * @param id        目标 ID
 * @param userId    所属用户 ID
 * @param title     岗位标题
 * @param jd        JD 原文
 * @param status    状态：active / archived
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record InterviewTargetDto(
        String id,
        String userId,
        String title,
        String jd,
        String status,
        String createdAt,
        String updatedAt
) {
}
