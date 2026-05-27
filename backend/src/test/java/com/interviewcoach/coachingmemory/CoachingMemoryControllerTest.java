package com.interviewcoach.coachingmemory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AssessmentAnswerRequest;
import com.interviewcoach.common.api.AssessmentStartRequest;
import com.interviewcoach.common.api.CandidateProfileConfirmRequest;
import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoachingMemoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCanCorrectAndRejectOwnMemoryItems() throws Exception {
        String token = loginAndGetToken("memory_correct_owner");
        String targetId = createTarget(token);
        confirmProfile(token, targetId);
        completeAssessment(token, targetId);
        String memoryId = firstMemoryId(token, targetId);

        mockMvc.perform(patch("/api/coaching-memories/" + memoryId + "/corrections")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "field": "observedWeaknesses",
                                  "itemIndex": 0,
                                  "source": "corrected",
                                  "content": "实际短板是回答缺少容量估算，而不是没有项目经验。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observedWeaknesses[0].source").value("corrected"))
                .andExpect(jsonPath("$.observedWeaknesses[0].confidence").value("high"))
                .andExpect(jsonPath("$.observedWeaknesses[0].content")
                        .value("实际短板是回答缺少容量估算，而不是没有项目经验。"));

        mockMvc.perform(patch("/api/coaching-memories/" + memoryId + "/corrections")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "field": "unverifiedClaims",
                                  "itemIndex": 0,
                                  "source": "rejected",
                                  "content": "否认：候选人没有负责过跨境支付清结算。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unverifiedClaims[0].source").value("rejected"))
                .andExpect(jsonPath("$.unverifiedClaims[0].confidence").value("high"))
                .andExpect(jsonPath("$.unverifiedClaims[0].content")
                        .value("否认：候选人没有负责过跨境支付清结算。"));
    }

    @Test
    void userCannotCorrectOtherUsersMemory() throws Exception {
        String tokenA = loginAndGetToken("memory_correct_a");
        String targetId = createTarget(tokenA);
        confirmProfile(tokenA, targetId);
        completeAssessment(tokenA, targetId);
        String memoryId = firstMemoryId(tokenA, targetId);

        String tokenB = loginAndGetToken("memory_correct_b");

        mockMvc.perform(patch("/api/coaching-memories/" + memoryId + "/corrections")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "field": "observedWeaknesses",
                                  "itemIndex": 0,
                                  "source": "corrected",
                                  "content": "其他用户不能改写这条记忆。"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COACHING_MEMORY_NOT_FOUND"));
    }

    @Test
    void correctionRejectsInvalidSource() throws Exception {
        String token = loginAndGetToken("memory_correct_invalid");
        String targetId = createTarget(token);
        confirmProfile(token, targetId);
        completeAssessment(token, targetId);
        String memoryId = firstMemoryId(token, targetId);

        mockMvc.perform(patch("/api/coaching-memories/" + memoryId + "/corrections")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "field": "observedWeaknesses",
                                  "itemIndex": 0,
                                  "source": "observed",
                                  "content": "纠错只能写入 corrected 或 rejected。"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private String loginAndGetToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String createTarget(String token) throws Exception {
        String response = mockMvc.perform(post("/api/targets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InterviewTargetCreateRequest(
                                "Java 后端工程师",
                                "负责支付系统订单、库存和对账链路，要求熟悉 Spring Boot、Redis、消息队列和高并发设计。"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void confirmProfile(String token, String targetId) throws Exception {
        mockMvc.perform(post("/api/profiles/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CandidateProfileConfirmRequest(
                                targetId,
                                "候选人做过 Java 支付订单系统，负责接口设计、异步消息和 Redis 缓存。",
                                List.of("Java", "Spring Boot", "Redis", "Kafka"),
                                List.of("支付订单系统"),
                                List.of("3 年后端开发经验")))))
                .andExpect(status().isOk());
    }

    private void completeAssessment(String token, String targetId) throws Exception {
        String startResponse = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(targetId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(startResponse).get("id").asText();

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest(
                                    "第 " + i + " 题回答：我会先说明业务目标，再讲方案、权衡和失败处理。"))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String firstMemoryId(String token, String targetId) throws Exception {
        String response = mockMvc.perform(get("/api/coaching-memories/target/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isString())
                .andReturn().getResponse().getContentAsString();
        JsonNode memories = objectMapper.readTree(response);
        return memories.get(0).get("id").asText();
    }
}
