package com.interviewcoach.training;

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
class TrainingControllerTest {

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

    private void completeAssessment(String token, String targetId) throws Exception {
        var startReq = new AssessmentStartRequest(targetId);
        String startResponse = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(startResponse).get("id").asText();

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest("Answer " + i))))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String generatePlanAndGetFirstTaskId(String token, String targetId) throws Exception {
        var generateReq = new TrainingPlanGenerateRequest(targetId);
        String planResponse = mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.targetId").value(targetId))
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks.length()").value(3))
                .andExpect(jsonPath("$.tasks[0].status").value("pending"))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(planResponse).get("tasks").get(0).get("id").asText();
    }

    @Test
    void fullTrainingLifecycle() throws Exception {
        String token = loginAndGetToken("train_user1");
        String targetId = createTarget(token, "Backend Engineer");
        confirmProfile(token, targetId);
        completeAssessment(token, targetId);

        // Generate plan
        String taskId = generatePlanAndGetFirstTaskId(token, targetId);

        // Get plan by targetId
        mockMvc.perform(get("/api/training-plans/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(targetId))
                .andExpect(jsonPath("$.tasks.length()").value(3));

        // Submit answer
        mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingTaskAnswerRequest("My answer to the training task"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.feedback").isString())
                .andExpect(jsonPath("$.problems").isArray())
                .andExpect(jsonPath("$.rewrittenAnswer").isString())
                .andExpect(jsonPath("$.followUpQuestion").isString())
                .andExpect(jsonPath("$.recommendedReviewPoints").isArray());

        // Complete task
        mockMvc.perform(patch("/api/training-tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.completedAt").isString());

        // Get plan again - task should be completed
        mockMvc.perform(get("/api/training-plans/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].status").value("completed"));

        // Regenerate plan — old plan should be deleted, new plan should be singular
        String newPlanResponse = mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingPlanGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        String newPlanId = objectMapper.readTree(newPlanResponse).get("id").asText();

        // GET should return the new plan without errors
        mockMvc.perform(get("/api/training-plans/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newPlanId))
                .andExpect(jsonPath("$.tasks.length()").value(3));
    }

    @Test
    void trainingRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/training-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/training-plans/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/training-tasks/00000000-0000-0000-0000-000000000000/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"test\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/training-tasks/00000000-0000-0000-0000-000000000000/complete"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessOtherUsersTraining() throws Exception {
        // User A generates plan
        String tokenA = loginAndGetToken("train_userA");
        String targetIdA = createTarget(tokenA, "User A Target");
        confirmProfile(tokenA, targetIdA);
        completeAssessment(tokenA, targetIdA);
        String taskId = generatePlanAndGetFirstTaskId(tokenA, targetIdA);

        // User B tries to access User A's training
        String tokenB = loginAndGetToken("train_userB");

        mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingTaskAnswerRequest("hack"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRAINING_NOT_FOUND"));

        mockMvc.perform(patch("/api/training-tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotGeneratePlanWithoutAssessment() throws Exception {
        String token = loginAndGetToken("train_user_noassess");
        String targetId = createTarget(token, "No Assessment Target");
        confirmProfile(token, targetId);

        mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingPlanGenerateRequest(targetId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void cannotAnswerTwice() throws Exception {
        String token = loginAndGetToken("train_user2");
        String targetId = createTarget(token, "Double Answer Test");
        confirmProfile(token, targetId);
        completeAssessment(token, targetId);
        String taskId = generatePlanAndGetFirstTaskId(token, targetId);

        // First answer succeeds
        mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingTaskAnswerRequest("First answer"))))
                .andExpect(status().isOk());

        // Second answer rejected
        mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingTaskAnswerRequest("Second answer"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void cannotCompleteWithoutAnswering() throws Exception {
        String token = loginAndGetToken("train_user3");
        String targetId = createTarget(token, "No Answer Test");
        confirmProfile(token, targetId);
        completeAssessment(token, targetId);
        String taskId = generatePlanAndGetFirstTaskId(token, targetId);

        // Try to complete without answering
        mockMvc.perform(patch("/api/training-tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void deleteTargetCascadesTraining() throws Exception {
        String token = loginAndGetToken("train_user4");
        String targetId = createTarget(token, "Cascade Test Target");
        confirmProfile(token, targetId);
        completeAssessment(token, targetId);
        String taskId = generatePlanAndGetFirstTaskId(token, targetId);

        // Submit answer on one task
        mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingTaskAnswerRequest("Answer"))))
                .andExpect(status().isOk());

        // Delete target
        mockMvc.perform(delete("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Plan should be gone
        mockMvc.perform(get("/api/training-plans/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
