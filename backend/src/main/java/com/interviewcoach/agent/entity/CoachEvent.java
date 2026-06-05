package com.interviewcoach.agent.entity;

/**
 * 教练事件类型枚举。定义触发教练 Agent 决策的业务事件，
 * 每个事件对应候选人面试准备流程中的一个关键节点。
 */
public enum CoachEvent {
    /** 目标岗位已创建 */
    TARGET_CREATED,
    /** 简历摘要已确认 */
    RESUME_SUMMARY_CONFIRMED,
    /** 测评已完成 */
    ASSESSMENT_COMPLETED,
    /** 单个训练任务已完成 */
    TRAINING_TASK_COMPLETED,
    /** 训练会话已全部完成 */
    TRAINING_SESSION_COMPLETED,
    /** 模拟面试已完成 */
    MOCK_INTERVIEW_COMPLETED,
    /** 用户进行了记忆纠错 */
    MEMORY_CORRECTED,
    /** App 会话已启动 */
    APP_SESSION_STARTED
}
