package com.interviewcoach.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.CandidateProfileDraftRequest;
import com.interviewcoach.common.api.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DraftSummaryAiTest {

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

    @Test
    void draftSummaryReturnsStructuredAiOutput() throws Exception {
        String token = loginAndGetToken("draft_test_user1");
        var request = new CandidateProfileDraftRequest(
                "5年Java后端开发经验，熟悉Spring Boot、MySQL、Redis",
                "负责订单系统重构，QPS从500提升到2000");

        String response = mockMvc.perform(post("/api/profiles/draft-summary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(response);
        assertFalse(json.get("summary").asText().isEmpty(), "summary must not be empty");
        assertTrue(json.get("skills").isArray());
        assertTrue(json.get("projects").isArray());
        assertTrue(json.get("experience").isArray());
        assertTrue(json.get("rawTextLength").asInt() > 0, "rawTextLength must be computed by backend");
    }

    @Test
    void draftSummaryComputesRawTextLengthFromBothFields() throws Exception {
        String token = loginAndGetToken("draft_test_user2");
        var request = new CandidateProfileDraftRequest("abc", "defgh");

        String response = mockMvc.perform(post("/api/profiles/draft-summary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(response);
        assertEquals(8, json.get("rawTextLength").asInt(), "rawTextLength = 3 + 5 = 8");
    }

    @Test
    void draftSummaryAcceptsEmptyInput() throws Exception {
        String token = loginAndGetToken("draft_test_user3");
        var request = new CandidateProfileDraftRequest(null, null);

        mockMvc.perform(post("/api/profiles/draft-summary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
