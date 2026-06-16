package com.interviewcoach.target.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.InterviewTargetUpdateRequest;
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
class TargetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username) throws Exception {
        var request = new com.interviewcoach.common.api.LoginRequest(username);
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void createTargetReturns201() throws Exception {
        String token = loginAndGetToken("target_user1");
        var request = new InterviewTargetCreateRequest("Java Backend Engineer", "5 years experience required");

        mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java Backend Engineer"))
                .andExpect(jsonPath("$.jd").value("5 years experience required"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.userId").isString());
    }

    @Test
    void createTargetWithoutJdReturnsValidationError() throws Exception {
        String token = loginAndGetToken("target_user_missing_jd");

        mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java Backend Engineer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("jd must not be blank"))
                .andExpect(jsonPath("$.requestId").isString());
    }

    @Test
    void listTargetsReturnsEmptyForNewUser() throws Exception {
        String token = loginAndGetToken("target_user2");

        mockMvc.perform(get("/api/targets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createAndGetTarget() throws Exception {
        String token = loginAndGetToken("target_user3");
        var request = new InterviewTargetCreateRequest("Payment Platform Dev", "Spring Boot + PostgreSQL");

        String createResponse = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String targetId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId))
                .andExpect(jsonPath("$.title").value("Payment Platform Dev"));
    }

    @Test
    void updateTarget() throws Exception {
        String token = loginAndGetToken("target_user4");
        var createRequest = new InterviewTargetCreateRequest("Original Title", "Original JD");

        String createResponse = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        String targetId = objectMapper.readTree(createResponse).get("id").asText();

        var updateRequest = new InterviewTargetUpdateRequest("Updated Title", null, null);

        mockMvc.perform(patch("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.jd").value("Original JD"));
    }

    @Test
    void deleteTarget() throws Exception {
        String token = loginAndGetToken("target_user5");
        var createRequest = new InterviewTargetCreateRequest("To Delete", "Will be deleted");

        String createResponse = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        String targetId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(delete("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TARGET_NOT_FOUND"));
    }

    @Test
    void targetsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/targets"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/targets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"jd\":\"Test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateWithInvalidStatusReturns400() throws Exception {
        String token = loginAndGetToken("target_user6");
        var createRequest = new InterviewTargetCreateRequest("Status Test", "JD");

        String createResponse = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        String targetId = objectMapper.readTree(createResponse).get("id").asText();

        var updateRequest = new InterviewTargetUpdateRequest(null, null, "banana");

        mockMvc.perform(patch("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void userCannotAccessOtherUsersTarget() throws Exception {
        // User A creates a target
        String tokenA = loginAndGetToken("target_userA");
        var createRequest = new InterviewTargetCreateRequest("User A Target", "Private");

        String createResponse = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        String targetId = objectMapper.readTree(createResponse).get("id").asText();

        // User B tries to access User A's target
        String tokenB = loginAndGetToken("target_userB");

        mockMvc.perform(get("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
