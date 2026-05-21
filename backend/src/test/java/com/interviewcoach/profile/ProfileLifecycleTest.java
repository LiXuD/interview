package com.interviewcoach.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.CandidateProfileConfirmRequest;
import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileLifecycleTest {

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

    private String createTarget(String token, String title) throws Exception {
        var request = new InterviewTargetCreateRequest(title, "Sample JD");
        String response = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void confirmProfile(String token, String targetId) throws Exception {
        var request = new CandidateProfileConfirmRequest(
                targetId, "Summary", List.of("Java"), List.of("Project A"), List.of("Company X"));
        mockMvc.perform(post("/api/profiles/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTargetAfterProfileConfirmSucceeds() throws Exception {
        String token = loginAndGetToken("lifecycle_user1");
        String targetId = createTarget(token, "Target With Profile");
        confirmProfile(token, targetId);

        // Delete target — should succeed despite existing profile
        mockMvc.perform(delete("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Target is gone
        mockMvc.perform(get("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // Profile is also gone
        mockMvc.perform(get("/api/profiles/current?targetId=" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUserAfterProfileConfirmSucceeds() throws Exception {
        String token = loginAndGetToken("lifecycle_user2");
        String targetId = createTarget(token, "Target For Deletion");
        confirmProfile(token, targetId);

        // Delete user — should cascade delete profiles and targets
        mockMvc.perform(delete("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Old token no longer works
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessOtherUsersProfile() throws Exception {
        // User A creates target and confirms profile
        String tokenA = loginAndGetToken("lifecycle_userA");
        String targetId = createTarget(tokenA, "User A Target");
        confirmProfile(tokenA, targetId);

        // User B tries to access User A's profile by targetId
        String tokenB = loginAndGetToken("lifecycle_userB");

        mockMvc.perform(get("/api/profiles/current?targetId=" + targetId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
