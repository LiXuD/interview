package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 当指定目标岗位下找不到 InterviewCoachAgent 时抛出。
 */
public class AgentNotFoundException extends RuntimeException {

    /**
     * 创建指定目标岗位下找不到 Agent 的异常实例。
     *
     * @param targetId 目标岗位 ID
     */
    public AgentNotFoundException(UUID targetId) {
        super("InterviewCoachAgent not found for target: " + targetId);
    }
}
