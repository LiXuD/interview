package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据 ID 查找目标岗位未找到时抛出。
 */
public class TargetNotFoundException extends RuntimeException {
    /**
     * 创建指定 ID 的目标岗位未找到的异常实例。
     *
     * @param targetId 目标岗位 ID
     */
    public TargetNotFoundException(UUID targetId) {
        super("Target not found: " + targetId);
    }
}
