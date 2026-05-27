package com.interviewcoach.ai.service;

public record AiPrompt(String task, String targetId, String systemPrompt, String userPrompt) {

    public static final String TASK_JOB_BRIEF = "jobBrief";
    public static final String TASK_ASSESSMENT_QUESTIONS = "assessmentQuestions";
    public static final String TASK_ASSESSMENT_RESULT = "assessmentResult";
    public static final String TASK_TRAINING_PLAN = "trainingPlan";
    public static final String TASK_TRAINING_FEEDBACK = "trainingFeedback";
    public static final String TASK_MOCK_INTERVIEW_QUESTION = "mockInterviewQuestion";
    public static final String TASK_MOCK_INTERVIEW_REPORT = "mockInterviewReport";
    public static final String TASK_CANDIDATE_PROFILE_DRAFT = "candidateProfileDraft";
}
