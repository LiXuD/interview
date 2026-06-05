package com.interviewcoach.common.api;

/**
 * 统一复盘报告 DTO，由测评完成或模拟面试完成自动生成。
 *
 * @param id        报告 ID
 * @param targetId  关联的岗位目标 ID
 * @param type      报告类型：assessment / mockInterview
 * @param content   报告内容（JSON 字符串）
 * @param createdAt 创建时间
 */
public record ReportDto(
        String id,
        String targetId,
        String type,
        String content,
        String createdAt
) {
}
