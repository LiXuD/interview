package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据 ID 查找模拟面试会话未找到时抛出。
 */
public class MockInterviewNotFoundException extends RuntimeException {
    /**
     * 创建指定 ID 的模拟面试会话未找到的异常实例。
     *
     * @param id 模拟面试会话 ID
     */
    public MockInterviewNotFoundException(UUID id) {
        super("Mock interview not found: " + id);
    }
}
