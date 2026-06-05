package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据目标岗位 ID 查找岗位画像未找到时抛出。
 */
public class JobBriefNotFoundException extends RuntimeException {
    /**
     * 创建指定目标岗位下找不到岗位画像的异常实例。
     *
     * @param targetId 目标岗位 ID
     */
    public JobBriefNotFoundException(UUID targetId) {
        super("Job brief not found for target: " + targetId);
    }
}
