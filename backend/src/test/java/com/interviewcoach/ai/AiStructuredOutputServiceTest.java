package com.interviewcoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.error.AiParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiStructuredOutputServiceTest {

    private AiStructuredOutputService serviceWith(PlatformAiClient client) {
        return new AiStructuredOutputService(client, null, null, null, new ObjectMapper());
    }

    // --- mockInterviewReport ---

    @Test
    void mockInterviewReportAcceptsMismatchedId() {
        PlatformAiClient client = prompt -> """
                {
                  "mockInterviewId": "wrong-id",
                  "overallScore": 70,
                  "dimensionScores": [
                    {"name": "technical depth", "score": 70, "reason": "reason"}
                  ],
                  "summary": "summary",
                  "strengths": [],
                  "weaknesses": [],
                  "improvedAnswers": [],
                  "nextTrainingTasks": []
                }
                """;
        var result = serviceWith(client).generateMockInterviewReport(
                new AiPrompt("mockInterviewReport", "expected-id", "system", "user"));
        assertEquals(70, result.overallScore());
        assertEquals("summary", result.summary());
    }

    @Test
    void mockInterviewReportRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewReport(
                new AiPrompt("mockInterviewReport", "id-1", "system", "user")));
        assertTrue(ex.getMessage().contains("mockInterviewReport"));
    }

    @Test
    void mockInterviewReportRejectsMissingSummary() {
        PlatformAiClient client = prompt -> """
                {
                  "mockInterviewId": "id-1",
                  "overallScore": 70,
                  "dimensionScores": [
                    {"name": "tech", "score": 70, "reason": "r"}
                  ],
                  "summary": "",
                  "strengths": [],
                  "weaknesses": [],
                  "improvedAnswers": [],
                  "nextTrainingTasks": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewReport(
                new AiPrompt("mockInterviewReport", "id-1", "system", "user")));
    }

    // --- candidateProfileDraft ---

    @Test
    void candidateProfileDraftRejectsMissingSummary() {
        PlatformAiClient client = prompt -> """
                {
                  "summary": "",
                  "skills": ["Java"],
                  "projects": [],
                  "experience": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateCandidateProfileDraft(
                new AiPrompt("candidateProfileDraft", null, "system", "user"), 100));
    }

    @Test
    void candidateProfileDraftRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not json at all";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateCandidateProfileDraft(
                new AiPrompt("candidateProfileDraft", null, "system", "user"), 100));
        assertTrue(ex.getMessage().contains("candidateProfileDraft"));
    }

    @Test
    void candidateProfileDraftUsesBackendRawTextLength() {
        PlatformAiClient client = prompt -> """
                {
                  "summary": "候选人有丰富经验",
                  "skills": ["Java", "Spring Boot"],
                  "projects": ["支付系统"],
                  "experience": ["3年后端经验"],
                  "rawTextLength": 999
                }
                """;
        CandidateProfileDraftDto result = serviceWith(client).generateCandidateProfileDraft(
                new AiPrompt("candidateProfileDraft", null, "system", "user"), 1234);

        assertEquals("候选人有丰富经验", result.summary());
        assertEquals(List.of("Java", "Spring Boot"), result.skills());
        assertEquals(1234, result.rawTextLength(), "rawTextLength must be computed by backend, not from AI");
    }

    @Test
    void candidateProfileDraftAllowsEmptyArrays() {
        PlatformAiClient client = prompt -> """
                {
                  "summary": "候选人信息有限",
                  "skills": [],
                  "projects": [],
                  "experience": []
                }
                """;
        CandidateProfileDraftDto result = serviceWith(client).generateCandidateProfileDraft(
                new AiPrompt("candidateProfileDraft", null, "system", "user"), 0);

        assertEquals("候选人信息有限", result.summary());
        assertTrue(result.skills().isEmpty());
    }

    // --- jobBrief ---

    @Test
    void jobBriefAcceptsMismatchedTargetId() {
        PlatformAiClient client = prompt -> """
                {
                  "targetId": "wrong-id",
                  "roleSummary": "Backend engineer role",
                  "skillMap": [{"name": "Java", "importance": "required", "userLevel": "unknown", "gap": "need practice"}],
                  "mustHaveSkills": ["Java"],
                  "niceToHaveSkills": [],
                  "businessContext": [],
                  "interviewTopics": [],
                  "candidateMatch": [],
                  "riskAreas": [],
                  "confidence": 0.6
                }
                """;
        var result = serviceWith(client).generateJobBrief(
                new AiPrompt("jobBrief", "expected-id", "system", "user"));
        assertEquals("Backend engineer role", result.roleSummary());
    }

    @Test
    void jobBriefRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "{ broken json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateJobBrief(
                new AiPrompt("jobBrief", "id-1", "system", "user")));
        assertTrue(ex.getMessage().contains("jobBrief"));
    }

    @Test
    void jobBriefRejectsMissingRoleSummary() {
        PlatformAiClient client = prompt -> """
                {
                  "targetId": "id-1",
                  "roleSummary": "",
                  "skillMap": [],
                  "mustHaveSkills": [],
                  "niceToHaveSkills": [],
                  "businessContext": [],
                  "interviewTopics": [],
                  "candidateMatch": [],
                  "riskAreas": [],
                  "confidence": 0.5
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateJobBrief(
                new AiPrompt("jobBrief", "id-1", "system", "user")));
    }

    @Test
    void jobBriefRejectsInvalidConfidence() {
        PlatformAiClient client = prompt -> """
                {
                  "targetId": "id-1",
                  "roleSummary": "Backend role",
                  "skillMap": [],
                  "mustHaveSkills": [],
                  "niceToHaveSkills": [],
                  "businessContext": [],
                  "interviewTopics": [],
                  "candidateMatch": [],
                  "riskAreas": [],
                  "confidence": 1.5
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateJobBrief(
                new AiPrompt("jobBrief", "id-1", "system", "user")));
    }

    // --- assessmentQuestions ---

    @Test
    void assessmentQuestionsRejectsWrongCount() {
        PlatformAiClient client = prompt -> """
                {
                  "questions": ["q1", "q2"]
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateAssessmentQuestions(
                new AiPrompt("assessmentQuestions", null, "system", "user")));
    }

    @Test
    void assessmentQuestionsRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateAssessmentQuestions(
                new AiPrompt("assessmentQuestions", null, "system", "user")));
        assertTrue(ex.getMessage().contains("assessmentQuestions"));
    }

    // --- assessmentResult ---

    @Test
    void assessmentResultAcceptsMismatchedId() {
        PlatformAiClient client = prompt -> """
                {
                  "assessmentId": "wrong-id",
                  "totalScore": 70,
                  "dimensions": [{"name": "Java", "score": 70, "reason": "good"}],
                  "strengths": [],
                  "weaknesses": [],
                  "nextActions": []
                }
                """;
        var result = serviceWith(client).generateAssessmentResult(
                new AiPrompt("assessmentResult", "expected-id", "system", "user"));
        assertEquals(70, result.totalScore());
    }

    @Test
    void assessmentResultRejectsScoreOutOfRange() {
        PlatformAiClient client = prompt -> """
                {
                  "assessmentId": "id-1",
                  "totalScore": 150,
                  "dimensions": [],
                  "strengths": [],
                  "weaknesses": [],
                  "nextActions": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateAssessmentResult(
                new AiPrompt("assessmentResult", "id-1", "system", "user")));
    }

    // --- trainingFeedback ---

    @Test
    void trainingFeedbackAcceptsMismatchedTaskId() {
        PlatformAiClient client = prompt -> """
                {
                  "taskId": "wrong-task-id",
                  "score": 70,
                  "feedback": "Good answer but needs more detail",
                  "problems": ["lacks specifics"],
                  "rewrittenAnswer": "Improved answer here",
                  "followUpQuestion": "Can you elaborate?",
                  "recommendedReviewPoints": ["topic A"]
                }
                """;
        var result = serviceWith(client).generateTrainingFeedback(
                new AiPrompt("trainingFeedback", "expected-task-id", "system", "user"));
        assertEquals(70, result.score());
        assertEquals("Good answer but needs more detail", result.feedback());
    }

    @Test
    void trainingFeedbackRejectsScoreOutOfRange() {
        PlatformAiClient client = prompt -> """
                {
                  "taskId": "task-1",
                  "score": 200,
                  "feedback": "feedback",
                  "problems": [],
                  "rewrittenAnswer": "rewritten",
                  "followUpQuestion": "follow up?",
                  "recommendedReviewPoints": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingFeedback(
                new AiPrompt("trainingFeedback", "task-1", "system", "user")));
    }

    @Test
    void trainingFeedbackRejectsMissingFeedback() {
        PlatformAiClient client = prompt -> """
                {
                  "taskId": "task-1",
                  "score": 70,
                  "feedback": "",
                  "problems": [],
                  "rewrittenAnswer": "rewritten",
                  "followUpQuestion": "follow up?",
                  "recommendedReviewPoints": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingFeedback(
                new AiPrompt("trainingFeedback", "task-1", "system", "user")));
    }

    @Test
    void trainingFeedbackRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "{invalid}";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingFeedback(
                new AiPrompt("trainingFeedback", "task-1", "system", "user")));
        assertTrue(ex.getMessage().contains("trainingFeedback"));
    }

    // --- trainingPlan ---

    @Test
    void trainingPlanRejectsTooFewTasks() {
        PlatformAiClient client = prompt -> """
                {
                  "tasks": [{"title": "Only one task", "description": "desc"}]
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingPlan(
                new AiPrompt("trainingPlan", null, "system", "user")));
    }

    @Test
    void trainingPlanRejectsMissingTitle() {
        PlatformAiClient client = prompt -> """
                {
                  "tasks": [
                    {"title": "", "description": "desc1"},
                    {"title": "t2", "description": "desc2"}
                  ]
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingPlan(
                new AiPrompt("trainingPlan", null, "system", "user")));
    }

    // --- mockInterviewQuestion ---

    @Test
    void mockInterviewQuestionRejectsEmptyQuestion() {
        PlatformAiClient client = prompt -> """
                {
                  "question": ""
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewQuestion(
                new AiPrompt("mockInterviewQuestion", null, "system", "user")));
    }

    @Test
    void mockInterviewQuestionRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "???";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewQuestion(
                new AiPrompt("mockInterviewQuestion", null, "system", "user")));
        assertTrue(ex.getMessage().contains("mockInterviewQuestion"));
    }

    // --- coachingMemory ---

    @Test
    void coachingMemoryRejectsInvalidSource() {
        PlatformAiClient client = prompt -> """
                {
                  "observedStrengths": [{"content": "strong", "source": "guessed", "confidence": "high"}],
                  "observedWeaknesses": [],
                  "recurringProblems": [],
                  "verifiedExperience": [],
                  "unverifiedClaims": [],
                  "recommendedNextFocus": [],
                  "avoidRepeating": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateCoachingMemory(
                new AiPrompt("coachingMemory", "target-1", "system", "user")));
    }

    @Test
    void coachingMemoryRejectsLegacyStringItems() {
        PlatformAiClient client = prompt -> """
                {
                  "observedStrengths": ["legacy string"],
                  "observedWeaknesses": [],
                  "recurringProblems": [],
                  "verifiedExperience": [],
                  "unverifiedClaims": [],
                  "recommendedNextFocus": [],
                  "avoidRepeating": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateCoachingMemory(
                new AiPrompt("coachingMemory", "target-1", "system", "user")));
    }

    // --- cross-cutting: retry on first failure ---

    @Test
    void generateAndValidateRetriesOnceOnInvalidJson() {
        int[] callCount = {0};
        PlatformAiClient client = prompt -> {
            callCount[0]++;
            if (callCount[0] == 1) {
                return "invalid json";
            }
            return """
                    {
                      "question": "How do you handle concurrency?"
                    }
                    """;
        };
        String result = serviceWith(client).generateMockInterviewQuestion(
                new AiPrompt("mockInterviewQuestion", null, "system", "user"));
        assertEquals("How do you handle concurrency?", result);
        assertEquals(2, callCount[0], "Should have retried once");
    }
}
