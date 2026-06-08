package com.interviewcoach.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.WechatLoginRequest;
import com.interviewcoach.common.security.WechatTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WechatLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WechatTokenVerifier wechatTokenVerifier;

    @Test
    void wechatLoginSuccessReturnsToken() throws Exception {
        when(wechatTokenVerifier.codeToOpenId("valid-code")).thenReturn("test-openid-123");

        WechatLoginRequest request = new WechatLoginRequest("valid-code");

        mockMvc.perform(post("/api/auth/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.userId").isString())
                .andExpect(jsonPath("$.username").value("wechat_test-openid-123"));
    }

    @Test
    void wechatLoginSuccessTokenCanAccessMe() throws Exception {
        when(wechatTokenVerifier.codeToOpenId("valid-code-2")).thenReturn("test-openid-456");

        WechatLoginRequest request = new WechatLoginRequest("valid-code-2");
        String response = mockMvc.perform(post("/api/auth/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("wechat_test-openid-456"));
    }

    @Test
    void wechatLoginSameOpenIdReusesUser() throws Exception {
        when(wechatTokenVerifier.codeToOpenId("reuse-code")).thenReturn("reuse-openid-789");

        WechatLoginRequest request = new WechatLoginRequest("reuse-code");

        String first = mockMvc.perform(post("/api/auth/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post("/api/auth/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String firstUserId = objectMapper.readTree(first).get("userId").asText();
        String secondUserId = objectMapper.readTree(second).get("userId").asText();

        // Same openId should return same user
        org.junit.jupiter.api.Assertions.assertEquals(firstUserId, secondUserId);
    }

    @Test
    void wechatLoginResponseDoesNotContainSessionKey() throws Exception {
        when(wechatTokenVerifier.codeToOpenId("any-code")).thenReturn("any-openid");

        WechatLoginRequest request = new WechatLoginRequest("any-code");
        String response = mockMvc.perform(post("/api/auth/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        // Response should only contain token, userId, username — no sessionKey
        org.junit.jupiter.api.Assertions.assertFalse(response.contains("sessionKey"));
        org.junit.jupiter.api.Assertions.assertFalse(response.contains("session_key"));
    }
}
