package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据 ID 查找 AI Provider 未找到时抛出。
 */
public class AiProviderNotFoundException extends RuntimeException {
    /**
     * 创建指定 ID 的 AI Provider 未找到的异常实例。
     *
     * @param id AI Provider ID
     */
    public AiProviderNotFoundException(UUID id) {
        super("AI provider not found: " + id);
    }
}
