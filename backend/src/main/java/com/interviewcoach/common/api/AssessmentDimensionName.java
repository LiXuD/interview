package com.interviewcoach.common.api;

import java.util.List;

public final class AssessmentDimensionName {

    public static final String TECHNICAL_DEPTH = "technicalDepth";
    public static final String PROJECT_SPECIFICITY = "projectSpecificity";
    public static final String SYSTEM_THINKING = "systemThinking";
    public static final String TRADEOFF_AWARENESS = "tradeoffAwareness";
    public static final String FAILURE_HANDLING = "failureHandling";
    public static final String COMMUNICATION_CLARITY = "communicationClarity";
    public static final String BUSINESS_CONTEXT = "businessContext";

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
