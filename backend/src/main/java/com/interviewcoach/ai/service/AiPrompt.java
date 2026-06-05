package com.interviewcoach.ai.service;

/**
 * AI 调用请求记录。封装任务类型、目标岗位 ID、系统提示词和用户提示词。
 * <p>各 TASK_* 常量对应不同的教练业务场景，用于指标采集和日志区分。</p>
 *
 * @param task         AI 任务类型标识
 * @param targetId     关联的目标岗位 ID
 * @param systemPrompt 系统提示词
 * @param userPrompt   用户提示词
 */
public record AiPrompt(String task, String targetId, String systemPrompt, String userPrompt) {

    public static final String TASK_JOB_BRIEF = "jobBrief";
    public static final String TASK_ASSESSMENT_QUESTIONS = "assessmentQuestions";
    public static final String TASK_ASSESSMENT_QUESTION_SCORE = "assessmentQuestionScore";
    public static final String TASK_ASSESSMENT_RESULT = "assessmentResult";
    public static final String TASK_TRAINING_PLAN = "trainingPlan";
    public static final String TASK_TRAINING_FEEDBACK = "trainingFeedback";
    public static final String TASK_ADAPTIVE_TRAINING_TURN = "adaptiveTrainingTurn";
    public static final String TASK_MOCK_INTERVIEW_QUESTION = "mockInterviewQuestion";
    public static final String TASK_MOCK_INTERVIEW_REPORT = "mockInterviewReport";
    public static final String TASK_CANDIDATE_PROFILE_DRAFT = "candidateProfileDraft";
    public static final String TASK_COACHING_MEMORY = "coachingMemory";
    public static final String TASK_AGENT_DECISION = "agentDecision";
}
