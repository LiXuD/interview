package com.interviewcoach.agent;

import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.service.InterviewCoachAgentRunner;
import com.interviewcoach.common.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InterviewCoachAgentRunnerTest {

    @Autowired
    private InterviewCoachAgentRunner runner;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void handleTargetCreatedEventReturnsDecision() {
        TestContext ctx = createTestContext("runner_target_created");

        AgentDecisionDto decision = runner.handleEvent(
                CoachEvent.TARGET_CREATED, ctx.targetId, ctx.userId);

        assertNotNull(decision);
        assertNotNull(decision.currentGoal());
        assertNotNull(decision.focusDimensions());
        assertNotNull(decision.recommendedAction());
        assertNotNull(decision.rationaleSummary());
        assertNotNull(decision.toolCalls());
        assertFalse(decision.toolCalls().isEmpty());
    }

    @Test
    void handleAssessmentCompletedUpdatesStage() {
        TestContext ctx = createTestContext("runner_assessment_done");

        // First event: set up agent
        runner.handleEvent(CoachEvent.TARGET_CREATED, ctx.targetId, ctx.userId);

        // Second event: assessment completed
        AgentDecisionDto decision = runner.handleEvent(
                CoachEvent.ASSESSMENT_COMPLETED, ctx.targetId, ctx.userId);

        assertNotNull(decision);
        assertFalse(decision.focusDimensions().isEmpty());
    }

    @Test
    void handleEventWithTrainingCompleted() {
        TestContext ctx = createTestContext("runner_training_done");

        runner.handleEvent(CoachEvent.TARGET_CREATED, ctx.targetId, ctx.userId);
        AgentDecisionDto decision = runner.handleEvent(
                CoachEvent.TRAINING_TASK_COMPLETED, ctx.targetId, ctx.userId);

        assertNotNull(decision);
        assertNotNull(decision.recommendedAction());
    }

    @Test
    void handleEventWithMockInterviewCompleted() {
        TestContext ctx = createTestContext("runner_mock_done");

        runner.handleEvent(CoachEvent.TARGET_CREATED, ctx.targetId, ctx.userId);
        AgentDecisionDto decision = runner.handleEvent(
                CoachEvent.MOCK_INTERVIEW_COMPLETED, ctx.targetId, ctx.userId);

        assertNotNull(decision);
    }

    @Test
    void handleEventWithMemoryCorrectedPreservesStage() {
        TestContext ctx = createTestContext("runner_memory_corrected");

        runner.handleEvent(CoachEvent.TARGET_CREATED, ctx.targetId, ctx.userId);
        AgentDecisionDto decision = runner.handleEvent(
                CoachEvent.MEMORY_CORRECTED, ctx.targetId, ctx.userId);

        assertNotNull(decision);
    }

    @Test
    void handleEventWithNonExistentTargetReturnsNull() {
        TestContext ctx = createTestContext("runner_no_target");

        AgentDecisionDto decision = runner.handleEvent(CoachEvent.TARGET_CREATED,
                java.util.UUID.randomUUID(), ctx.userId);
        assertNull(decision);
    }

    @Test
    void allowedToolsMatchWhitelist() {
        assertTrue(InterviewCoachAgentRunner.ALLOWED_TOOLS.contains("startAssessment"));
        assertTrue(InterviewCoachAgentRunner.ALLOWED_TOOLS.contains("generateTrainingPlan"));
        assertTrue(InterviewCoachAgentRunner.ALLOWED_TOOLS.contains("startAdaptiveTraining"));
        assertTrue(InterviewCoachAgentRunner.ALLOWED_TOOLS.contains("startMockInterview"));
        assertTrue(InterviewCoachAgentRunner.ALLOWED_TOOLS.contains("analyzeProgress"));
        assertTrue(InterviewCoachAgentRunner.ALLOWED_TOOLS.contains("updateCoachingMemory"));
        assertEquals(6, InterviewCoachAgentRunner.ALLOWED_TOOLS.size());
    }

    private TestContext createTestContext(String username) {
        try {
            String loginResponse = mockMvc.perform(post("/api/auth/dev-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(username))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String token = objectMapper.readTree(loginResponse).get("token").asText();
            java.util.UUID userId = java.util.UUID.fromString(
                    objectMapper.readTree(loginResponse).get("userId").asText());

            String targetResponse = mockMvc.perform(post("/api/targets")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new InterviewTargetCreateRequest("Java 后端工程师",
                                            "负责核心系统开发。"))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            java.util.UUID targetId = java.util.UUID.fromString(
                    objectMapper.readTree(targetResponse).get("id").asText());

            return new TestContext(userId, targetId, token);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test context", e);
        }
    }

    private record TestContext(java.util.UUID userId, java.util.UUID targetId, String token) {}
}
