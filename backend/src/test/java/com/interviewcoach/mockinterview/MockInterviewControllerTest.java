package com.interviewcoach.mockinterview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MockInterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username) throws Exception {
        var request = new LoginRequest(username);
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String createTarget(String token, String title) throws Exception {
        var request = new InterviewTargetCreateRequest(title, "Sample JD for testing");
        String response = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void confirmProfile(String token, String targetId) throws Exception {
        var profileReq = new CandidateProfileConfirmRequest(
                targetId, "Test summary", java.util.List.of("Java"), java.util.List.of("Project A"), java.util.List.of("Company X"));
        mockMvc.perform(post("/api/profiles/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isOk());
    }

    private String startInterviewAndGetId(String token, String targetId) throws Exception {
        String response = mockMvc.perform(post("/api/mock-interviews/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewStartRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.targetId").value(targetId))
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.currentQuestion").isString())
                .andExpect(jsonPath("$.conversationTurns").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void fullMockInterviewLifecycle() throws Exception {
        String token = loginAndGetToken("mi_user1");
        String targetId = createTarget(token, "Backend Engineer");
        confirmProfile(token, targetId);

        // Start interview
        String interviewId = startInterviewAndGetId(token, targetId);

        // Get session
        mockMvc.perform(get("/api/mock-interviews/" + interviewId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(interviewId))
                .andExpect(jsonPath("$.status").value("in_progress"));

        // Submit answer
        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewAnswerRequest("My answer to the question"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.currentQuestion").isString())
                .andExpect(jsonPath("$.conversationTurns").value(1));

        // Submit second answer
        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewAnswerRequest("Second answer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationTurns").value(2));

        // Finish interview
        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mockInterviewId").value(interviewId))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.dimensionScores").isArray())
                .andExpect(jsonPath("$.dimensionScores.length()").value(4))
                .andExpect(jsonPath("$.summary").isString())
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.weaknesses").isArray())
                .andExpect(jsonPath("$.improvedAnswers").isArray())
                .andExpect(jsonPath("$.nextTrainingTasks").isArray());

        // Get session — should be completed
        mockMvc.perform(get("/api/mock-interviews/" + interviewId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void mockInterviewRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/mock-interviews/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/mock-interviews/00000000-0000-0000-0000-000000000000/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"test\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/mock-interviews/00000000-0000-0000-0000-000000000000/finish"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/mock-interviews/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessOtherUsersInterview() throws Exception {
        // User A starts interview
        String tokenA = loginAndGetToken("mi_userA");
        String targetIdA = createTarget(tokenA, "User A Target");
        confirmProfile(tokenA, targetIdA);
        String interviewId = startInterviewAndGetId(tokenA, targetIdA);

        // User B tries to access User A's interview
        String tokenB = loginAndGetToken("mi_userB");

        mockMvc.perform(get("/api/mock-interviews/" + interviewId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MOCK_INTERVIEW_NOT_FOUND"));

        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewAnswerRequest("hack"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotFinishTwice() throws Exception {
        String token = loginAndGetToken("mi_user2");
        String targetId = createTarget(token, "Finish Twice Test");
        confirmProfile(token, targetId);
        String interviewId = startInterviewAndGetId(token, targetId);

        // First finish succeeds
        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Second finish rejected
        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void deleteTargetCascadesMockInterview() throws Exception {
        String token = loginAndGetToken("mi_user3");
        String targetId = createTarget(token, "Cascade Test Target");
        confirmProfile(token, targetId);
        String interviewId = startInterviewAndGetId(token, targetId);

        // Submit one answer
        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewAnswerRequest("Answer"))))
                .andExpect(status().isOk());

        // Delete target
        mockMvc.perform(delete("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Interview should be gone
        mockMvc.perform(get("/api/mock-interviews/" + interviewId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
