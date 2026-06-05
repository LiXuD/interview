package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据 ID 查找训练计划或训练任务未找到时抛出。
 */
public class TrainingNotFoundException extends RuntimeException {
    /**
     * 创建指定 ID 的训练计划或训练任务未找到的异常实例。
     *
     * @param id 训练计划/任务 ID
     */
    public TrainingNotFoundException(UUID id) {
        super("Training not found: " + id);
    }
}
