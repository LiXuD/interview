package com.interviewcoach.jobbrief;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.common.api.CandidateProfileConfirmRequest;
import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.JobBriefGenerateRequest;
import com.interviewcoach.jobbrief.repository.JobBriefRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobBriefControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlatformAiClient platformAiClient;

    @Autowired
    private JobBriefRepository jobBriefRepository;

    @Test
    void generateJobBriefParsesAiJsonAndPersistsResult() throws Exception {
        String token = loginAndGetToken("jobbrief_user1");
        String targetId = createTarget(token, "Java 后端工程师", "Spring Boot, PostgreSQL, Redis, payment platform");
        confirmProfile(token, targetId);

        Mockito.when(platformAiClient.generateJson(any())).thenReturn("""
                {
                  "targetId": "%s",
                  "roleSummary": "负责支付平台后端服务设计与交付。",
                  "skillMap": [
                    {"name": "Spring Boot", "importance": "required", "userLevel": "solid", "gap": "继续补强事务和安全设计"},
                    {"name": "Redis", "importance": "important", "userLevel": "basic", "gap": "需要准备缓存一致性案例"}
                  ],
                  "mustHaveSkills": ["Java", "Spring Boot", "SQL"],
                  "niceToHaveSkills": ["Redis", "支付系统经验"],
                  "businessContext": ["高可用支付链路", "资金安全"],
                  "interviewTopics": ["事务一致性", "接口幂等", "线上故障排查"],
                  "candidateMatch": ["Java 基础可迁移到岗位核心需求"],
                  "riskAreas": ["支付业务细节需要用户确认"],
                  "confidence": 0.82
                }
                """.formatted(targetId));

        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(targetId))
                .andExpect(jsonPath("$.roleSummary").value("负责支付平台后端服务设计与交付。"))
                .andExpect(jsonPath("$.skillMap[0].name").value("Spring Boot"))
                .andExpect(jsonPath("$.skillMap[0].importance").value("required"))
                .andExpect(jsonPath("$.mustHaveSkills[0]").value("Java"))
                .andExpect(jsonPath("$.confidence").value(0.82));

        mockMvc.perform(get("/api/job-briefs/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(targetId))
                .andExpect(jsonPath("$.interviewTopics[1]").value("接口幂等"));
    }

    @Test
    void generateJobBriefRequiresConfirmedProfile() throws Exception {
        String token = loginAndGetToken("jobbrief_user2");
        String targetId = createTarget(token, "Java 后端工程师", "Spring Boot");

        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void invalidAiJsonReturnsAiParseFailedAfterOneRetry() throws Exception {
        String token = loginAndGetToken("jobbrief_user3");
        String targetId = createTarget(token, "Java 后端工程师", "Spring Boot");
        confirmProfile(token, targetId);

        Mockito.when(platformAiClient.generateJson(any()))
                .thenReturn("{not valid json")
                .thenReturn("""
                        {
                          "targetId": "%s",
                          "roleSummary": "",
                          "skillMap": [],
                          "mustHaveSkills": [],
                          "niceToHaveSkills": [],
                          "businessContext": [],
                          "interviewTopics": [],
                          "candidateMatch": [],
                          "riskAreas": [],
                          "confidence": 1.5
                        }
                        """.formatted(targetId));

        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AI_PARSE_FAILED"))
                .andExpect(jsonPath("$.message").value("AI returned invalid structured output."))
                .andExpect(jsonPath("$.requestId").isString());

        Mockito.verify(platformAiClient, times(2)).generateJson(any());
    }

    @Test
    void userCannotAccessOtherUsersJobBrief() throws Exception {
        String tokenA = loginAndGetToken("jobbrief_userA");
        String targetId = createTarget(tokenA, "User A Target", "Private JD");
        confirmProfile(tokenA, targetId);

        Mockito.when(platformAiClient.generateJson(any())).thenReturn("""
                {
                  "targetId": "%s",
                  "roleSummary": "private",
                  "skillMap": [],
                  "mustHaveSkills": [],
                  "niceToHaveSkills": [],
                  "businessContext": [],
                  "interviewTopics": [],
                  "candidateMatch": [],
                  "riskAreas": [],
                  "confidence": 0.5
                }
                """.formatted(targetId));

        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isOk());

        String tokenB = loginAndGetToken("jobbrief_userB");
        mockMvc.perform(get("/api/job-briefs/" + targetId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTargetCleansJobBrief() throws Exception {
        String token = loginAndGetToken("jobbrief_delete_target_user");
        String targetId = createTarget(token, "Target To Delete", "Spring Boot");
        confirmProfile(token, targetId);
        mockValidJobBrief(targetId);

        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/targets/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(jobBriefRepository.findByTargetIdAndUserId(
                UUID.fromString(targetId),
                UUID.fromString(currentUserId(token))
        )).isEmpty();

        mockMvc.perform(get("/api/job-briefs/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUserCleansJobBrief() throws Exception {
        String token = loginAndGetToken("jobbrief_delete_user");
        String userId = currentUserId(token);
        String targetId = createTarget(token, "Target Before Account Deletion", "Spring Boot");
        confirmProfile(token, targetId);
        mockValidJobBrief(targetId);

        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(jobBriefRepository.findByTargetIdAndUserId(
                UUID.fromString(targetId),
                UUID.fromString(userId)
        )).isEmpty();
    }

    private String loginAndGetToken(String username) throws Exception {
        var request = new com.interviewcoach.common.api.LoginRequest(username);
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String createTarget(String token, String title, String jd) throws Exception {
        var request = new InterviewTargetCreateRequest(title, jd);
        String response = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String currentUserId(String token) throws Exception {
        String response = mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void mockValidJobBrief(String targetId) {
        Mockito.when(platformAiClient.generateJson(any())).thenReturn("""
                {
                  "targetId": "%s",
                  "roleSummary": "基于 JD 的岗位画像。",
                  "skillMap": [],
                  "mustHaveSkills": [],
                  "niceToHaveSkills": [],
                  "businessContext": [],
                  "interviewTopics": [],
                  "candidateMatch": [],
                  "riskAreas": [],
                  "confidence": 0.5
                }
                """.formatted(targetId));
    }

    private void confirmProfile(String token, String targetId) throws Exception {
        var request = new CandidateProfileConfirmRequest(
                targetId,
                "三年 Java 后端经验，做过支付订单和对账模块。",
                List.of("Java", "Spring Boot", "PostgreSQL"),
                List.of("支付订单系统"),
                List.of("后端开发工程师")
        );
        mockMvc.perform(post("/api/profiles/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
