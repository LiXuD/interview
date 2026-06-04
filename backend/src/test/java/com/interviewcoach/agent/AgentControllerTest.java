package com.interviewcoach.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAgentCreatesNewAgentIfNotExists() throws Exception {
        String token = loginAndGetToken("agent_create_user");
        String targetId = createTarget(token, "Java 后端工程师");

        mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.targetId").value(targetId))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.currentStage").value("targetSetup"))
                .andExpect(jsonPath("$.activeFocusDimensions").isArray())
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString());
    }

    @Test
    void getAgentReturnsSameAgentOnSecondCall() throws Exception {
        String token = loginAndGetToken("agent_idempotent_user");
        String targetId = createTarget(token, "AI 应用工程师");

        String firstResponse = mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String firstId = objectMapper.readTree(firstResponse).get("id").asText();

        String secondResponse = mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secondId = objectMapper.readTree(secondResponse).get("id").asText();

        assert firstId.equals(secondId) : "Agent should be the same on second call";
    }

    @Test
    void getAgentWithoutAuthReturns401() throws Exception {
        UUID randomTargetId = UUID.randomUUID();

        mockMvc.perform(get("/api/targets/" + randomTargetId + "/coach-agent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAgentForNonExistentTargetReturnsNotFound() throws Exception {
        String token = loginAndGetToken("agent_no_target_user");
        UUID nonExistentTargetId = UUID.randomUUID();

        mockMvc.perform(get("/api/targets/" + nonExistentTargetId + "/coach-agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TARGET_NOT_FOUND"));
    }

    @Test
    void userCannotAccessOtherUsersAgent() throws Exception {
        String tokenA = loginAndGetToken("agent_owner_user");
        String targetId = createTarget(tokenA, "数据平台工程师");

        // Create agent for user A
        mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // User B tries to access user A's target agent
        String tokenB = loginAndGetToken("agent_other_user");

        mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TARGET_NOT_FOUND"));
    }

    @Test
    void deleteAccountCascadesToAgents() throws Exception {
        String token = loginAndGetToken("agent_delete_user");
        String targetId = createTarget(token, "后端架构师");

        // Create agent
        mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Delete account
        mockMvc.perform(delete("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Login again with same username - should get new user, no old agent
        String newToken = loginAndGetToken("agent_delete_user");
        String newTargetId = createTarget(newToken, "后端架构师");

        // Old target should not be accessible
        mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TARGET_NOT_FOUND"));
    }

    @Test
    void agentDtoUsesCamelCase() throws Exception {
        String token = loginAndGetToken("agent_camelcase_user");
        String targetId = createTarget(token, "前端工程师");

        String response = mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        // Verify camelCase field names exist
        assert json.has("targetId") : "Should use camelCase targetId";
        assert json.has("currentStage") : "Should use camelCase currentStage";
        assert json.has("currentGoal") : "Should use camelCase currentGoal";
        assert json.has("activeFocusDimensions") : "Should use camelCase activeFocusDimensions";
        assert json.has("nextRecommendedAction") : "Should use camelCase nextRecommendedAction";
        assert json.has("lastEventType") : "Should use camelCase lastEventType";
        assert json.has("lastDecisionSummary") : "Should use camelCase lastDecisionSummary";
        assert json.has("lastRunAt") : "Should use camelCase lastRunAt";
        assert json.has("createdAt") : "Should have createdAt";
        assert json.has("updatedAt") : "Should have updatedAt";
        // Should NOT have snake_case
        assert !json.has("target_id") : "Should not have snake_case target_id";
        assert !json.has("current_stage") : "Should not have snake_case current_stage";
    }

    private String loginAndGetToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String createTarget(String token, String title) throws Exception {
        String response = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InterviewTargetCreateRequest(
                                title,
                                "负责核心系统开发，要求熟悉相关技术栈和高并发设计。"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}
