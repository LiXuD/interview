package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据 ID 查找用户未找到时抛出。
 */
public class UserNotFoundException extends RuntimeException {
    /**
     * 创建指定 ID 的用户未找到的异常实例。
     *
     * @param userId 用户 ID
     */
    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }
}
