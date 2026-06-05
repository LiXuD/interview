package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据 ID 查找复盘报告未找到时抛出。
 */
public class ReportNotFoundException extends RuntimeException {
    /**
     * 创建指定 ID 的复盘报告未找到的异常实例。
     *
     * @param reportId 报告 ID
     */
    public ReportNotFoundException(UUID reportId) {
        super("Report not found: " + reportId);
    }
}
