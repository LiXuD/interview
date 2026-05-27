package com.interviewcoach.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AssessmentAnswerRequest;
import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.LoginRequest;
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
class AssessmentControllerTest {

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
        var profileReq = new com.interviewcoach.common.api.CandidateProfileConfirmRequest(
                targetId, "Test summary", java.util.List.of("Java"), java.util.List.of("Project A"), java.util.List.of("Company X"));
        mockMvc.perform(post("/api/profiles/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isOk());
    }

    private String startAssessment(String token, String targetId) throws Exception {
        var request = new com.interviewcoach.common.api.AssessmentStartRequest(targetId);
        String response = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.questionIndex").value(0))
                .andExpect(jsonPath("$.totalQuestions").value(5))
                .andExpect(jsonPath("$.currentQuestion.question").isString())
                .andExpect(jsonPath("$.currentQuestion.dimension").isString())
                .andExpect(jsonPath("$.currentQuestion.difficulty").isString())
                .andExpect(jsonPath("$.currentQuestion.intent").isString())
                .andExpect(jsonPath("$.currentQuestion.rubric").isArray())
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions.length()").value(5))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void fullAssessmentLifecycle() throws Exception {
        String token = loginAndGetToken("assess_user1");
        String targetId = createTarget(token, "Backend Engineer");
        confirmProfile(token, targetId);

        // Start assessment
        String sessionId = startAssessment(token, targetId);

        // Answer 5 questions
        for (int i = 1; i <= 5; i++) {
            String answer = "Answer for question " + i;
            var answerReq = new AssessmentAnswerRequest(answer);
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(answerReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("in_progress"))
                    .andExpect(jsonPath("$.questionIndex").value(i));
        }

        // Finish assessment
        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentId").value(sessionId))
                .andExpect(jsonPath("$.totalScore").isNumber())
                .andExpect(jsonPath("$.dimensions").isArray())
                .andExpect(jsonPath("$.dimensions.length()").value(5))
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.weaknesses").isArray())
                .andExpect(jsonPath("$.nextActions").isArray());

        // Get session - should be completed
        mockMvc.perform(get("/api/assessments/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.currentQuestion").doesNotExist())
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions.length()").value(5));
    }

    @Test
    void assessmentRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/assessments/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/assessments/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessOtherUsersAssessment() throws Exception {
        // User A starts assessment
        String tokenA = loginAndGetToken("assess_userA");
        String targetId = createTarget(tokenA, "User A Target");
        confirmProfile(tokenA, targetId);
        String sessionId = startAssessment(tokenA, targetId);

        // User B tries to access User A's assessment
        String tokenB = loginAndGetToken("assess_userB");

        mockMvc.perform(get("/api/assessments/" + sessionId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_NOT_FOUND"));

        mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest("hack"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotAnswerCompletedAssessment() throws Exception {
        String token = loginAndGetToken("assess_user2");
        String targetId = createTarget(token, "Completed Test");
        confirmProfile(token, targetId);
        String sessionId = startAssessment(token, targetId);

        // Answer all 5 questions then finish
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest("A" + i))))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Try to answer completed assessment
        mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest("late"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void cannotFinishWithoutAnsweringAllQuestions() throws Exception {
        String token = loginAndGetToken("assess_user3");
        String targetId = createTarget(token, "Incomplete Test");
        confirmProfile(token, targetId);
        String sessionId = startAssessment(token, targetId);

        // Answer only 2 questions
        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest("A" + i))))
                    .andExpect(status().isOk());
        }

        // Try to finish early
        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void cannotSubmitMoreThanTotalQuestions() throws Exception {
        String token = loginAndGetToken("assess_user5");
        String targetId = createTarget(token, "Overflow Test");
        confirmProfile(token, targetId);
        String sessionId = startAssessment(token, targetId);

        // Answer all 5 questions
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest("A" + i))))
                    .andExpect(status().isOk());
        }

        // 6th answer should be rejected
        mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest("overflow"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void deleteTargetCascadesAssessment() throws Exception {
        String token = loginAndGetToken("assess_user4");
        String targetId = createTarget(token, "Cascade Test Target");
        confirmProfile(token, targetId);
        String sessionId = startAssessment(token, targetId);

        // Answer a couple questions
        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest("A" + i))))
                    .andExpect(status().isOk());
        }

        // Delete target - should succeed
        mockMvc.perform(delete("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Session should be gone
        mockMvc.perform(get("/api/assessments/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
