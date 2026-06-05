package com.interviewcoach.common.api;

import java.util.List;

/**
 * 测评能力维度名称常量，定义 7 个稳定评估维度。
 */
public final class AssessmentDimensionName {

    /** 技术深度 */
    public static final String TECHNICAL_DEPTH = "technicalDepth";
    /** 项目具体性 */
    public static final String PROJECT_SPECIFICITY = "projectSpecificity";
    /** 系统思维 */
    public static final String SYSTEM_THINKING = "systemThinking";
    /** 权衡意识 */
    public static final String TRADEOFF_AWARENESS = "tradeoffAwareness";
    /** 失败处理 */
    public static final String FAILURE_HANDLING = "failureHandling";
    /** 表达清晰度 */
    public static final String COMMUNICATION_CLARITY = "communicationClarity";
    /** 业务上下文 */
    public static final String BUSINESS_CONTEXT = "businessContext";

    /** 全部 7 个维度的有序列表 */
    public static final List<String> ALL = List.of(
            TECHNICAL_DEPTH,
            PROJECT_SPECIFICITY,
            SYSTEM_THINKING,
            TRADEOFF_AWARENESS,
            FAILURE_HANDLING,
            COMMUNICATION_CLARITY,
            BUSINESS_CONTEXT
    );

    private AssessmentDimensionName() {}
}
