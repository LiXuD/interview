package com.interviewcoach.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AdminTokenQuotaUpdateRequest;
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
class AdminUserQuotaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    private String adminToken;
    private UUID regularUserId;
    private String regularUsername;

    @BeforeEach
    void setUp() throws Exception {
        // admin 是 application-test.yml 中配置的管理员用户名
        adminToken = loginAndGetToken("admin");

        regularUsername = "reg_quota_" + System.nanoTime();
        loginAndGetToken(regularUsername);
        regularUserId = userRepository.findByUsername(regularUsername).orElseThrow().getId();
    }

    private String loginAndGetToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void adminCanSetQuota() throws Exception {
        AdminTokenQuotaUpdateRequest req = new AdminTokenQuotaUpdateRequest(1000000L);
        mockMvc.perform(patch("/api/admin/users/" + regularUserId + "/token-quota")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(regularUserId.toString()))
                .andExpect(jsonPath("$.monthlyTokenQuota").value(1000000))
                .andExpect(jsonPath("$.quotaExceeded").value(false));
    }

    @Test
    void adminCanSetNullQuotaForUnlimited() throws Exception {
        AdminTokenQuotaUpdateRequest req = new AdminTokenQuotaUpdateRequest(null);
        mockMvc.perform(patch("/api/admin/users/" + regularUserId + "/token-quota")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyTokenQuota").isEmpty())
                .andExpect(jsonPath("$.remainingMonthlyTokens").isEmpty());
    }

    @Test
    void adminCanSetZeroQuotaToBlock() throws Exception {
        AdminTokenQuotaUpdateRequest req = new AdminTokenQuotaUpdateRequest(0L);
        mockMvc.perform(patch("/api/admin/users/" + regularUserId + "/token-quota")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyTokenQuota").value(0));
    }

    @Test
    void adminCanGetQuota() throws Exception {
        mockMvc.perform(get("/api/admin/users/" + regularUserId + "/token-quota")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(regularUserId.toString()));
    }

    @Test
    void nonAdminCannotSetQuota() throws Exception {
        String userToken = loginAndGetToken(regularUsername);
        AdminTokenQuotaUpdateRequest req = new AdminTokenQuotaUpdateRequest(1000000L);
        mockMvc.perform(patch("/api/admin/users/" + regularUserId + "/token-quota")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotGetQuota() throws Exception {
        String userToken = loginAndGetToken(regularUsername);
        mockMvc.perform(get("/api/admin/users/" + regularUserId + "/token-quota")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
