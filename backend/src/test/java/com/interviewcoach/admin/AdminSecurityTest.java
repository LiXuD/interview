package com.interviewcoach.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username) throws Exception {
        LoginRequest request = new LoginRequest(username);
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void adminUserGetsRoleAdmin() throws Exception {
        String token = loginAndGetToken("admin");

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        String token = loginAndGetToken("admin");

        mockMvc.perform(get("/api/admin/ai-usage/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void regularUserCannotAccessAdminEndpoint() throws Exception {
        String token = loginAndGetToken("regularuser");

        mockMvc.perform(get("/api/admin/ai-usage/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void unauthenticatedCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserCanAccessNormalEndpoints() throws Exception {
        String token = loginAndGetToken("normaluser");

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("normaluser"));
    }

    @Test
    void secondAdminUserAlsoGetsRoleAdmin() throws Exception {
        String token = loginAndGetToken("testadmin");

        mockMvc.perform(get("/api/admin/ai-usage/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
