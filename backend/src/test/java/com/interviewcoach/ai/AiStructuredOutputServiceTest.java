package com.interviewcoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.common.error.AiParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AiStructuredOutputServiceTest {

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
        AiStructuredOutputService service = new AiStructuredOutputService(client, new ObjectMapper());

        assertThrows(AiParseException.class, () -> service.generateMockInterviewReport(
                new AiPrompt("mockInterviewReport", "expected-id", "system", "user")));
    }
}
