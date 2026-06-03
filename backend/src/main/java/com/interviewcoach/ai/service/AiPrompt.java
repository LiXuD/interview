package com.interviewcoach.ai.service;

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
