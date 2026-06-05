package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据 ID 查找测评会话未找到时抛出。
 */
public class AssessmentNotFoundException extends RuntimeException {
    /**
     * 创建指定 ID 的测评会话未找到的异常实例。
     *
     * @param assessmentId 测评会话 ID
     */
    public AssessmentNotFoundException(UUID assessmentId) {
        super("Assessment not found: " + assessmentId);
    }
}
