package com.interviewcoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.error.AiParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiStructuredOutputServiceTest {

    private AiStructuredOutputService serviceWith(PlatformAiClient client) {
        return new AiStructuredOutputService(client, null, null, null, new ObjectMapper());
    }

    @Test
    void mockInterviewReportRejectsMismatchedId() {
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
        assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewReport(
                new AiPrompt("mockInterviewReport", "expected-id", "system", "user")));
    }

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
        assertThrows(AiParseException.class, () -> serviceWith(client).generateCandidateProfileDraft(
                new AiPrompt("candidateProfileDraft", null, "system", "user"), 100));
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
}
