package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据 ID 查找教练记忆记录未找到时抛出。
 */
public class CoachingMemoryNotFoundException extends RuntimeException {

    /**
     * 创建指定 ID 的教练记忆记录未找到的异常实例。
     *
     * @param id 教练记忆记录 ID
     */
    public CoachingMemoryNotFoundException(UUID id) {
        super("CoachingMemory not found: " + id);
    }
}
