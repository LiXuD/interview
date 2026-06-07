package com.interviewcoach.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.aiusage.entity.AiUsageLog;
import com.interviewcoach.aiusage.repository.AiUsageLogRepository;
import com.interviewcoach.common.api.LoginRequest;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
class AdminAiUsageControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AiUsageLogRepository usageRepository;

    private String adminToken;
    private UUID regularUserId;
    private String regularUsername;

    @BeforeEach
    void setUp() throws Exception {
        usageRepository.deleteAll();

        // admin 是 application-test.yml 中配置的管理员用户名
        adminToken = loginAndGetToken("admin");

        regularUsername = "reg_usage_" + System.nanoTime();
        loginAndGetToken(regularUsername);
        regularUserId = userRepository.findByUsername(regularUsername).orElseThrow().getId();

        AiUsageLog log = new AiUsageLog();
        log.setUserId(regularUserId);
        log.setRequestId("req-" + System.nanoTime());
        log.setTask("jobBrief");
        log.setProviderType("platformDefault");
        log.setModel("gpt-4o");
        log.setMode("chatCompletions");
        log.setUsageSource("actual");
        log.setInputTokens(100);
        log.setOutputTokens(50);
        log.setTotalTokens(150);
        log.setSuccess(true);
        usageRepository.save(log);
    }

    private String loginAndGetToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void overviewReturnsData() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.summary.totalTokens").value(150))
                .andExpect(jsonPath("$.summary.totalRequests").value(1));
    }

    @Test
    void overviewWithDateFilter() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/overview")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalTokens").value(150));
    }

    @Test
    void usersPageReturnsUsers() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", regularUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void usersPageWithKeyword() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", regularUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].username").value(regularUsername));
    }

    @Test
    void userDetailReturnsDetail() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/users/" + regularUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(regularUsername))
                .andExpect(jsonPath("$.summary.totalTokens").value(150));
    }

    @Test
    void nonAdminGets403() throws Exception {
        String userToken = loginAndGetToken(regularUsername);
        mockMvc.perform(get("/api/admin/ai-usage/overview")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void unauthenticatedGets401() throws Exception {
        mockMvc.perform(get("/api/admin/ai-usage/overview"))
                .andExpect(status().isUnauthorized());
    }
}
