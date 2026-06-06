package com.interviewcoach.aiusage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.aiusage.entity.AiUsageLog;
import com.interviewcoach.aiusage.repository.AiUsageLogRepository;
import com.interviewcoach.common.api.LoginRequest;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiUsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiUsageLogRepository usageRepository;

    @Test
    void summaryOnlyAggregatesCurrentUserUsage() throws Exception {
        String tokenA = loginAndGetToken("usage_user_a");
        String tokenB = loginAndGetToken("usage_user_b");
        User userA = userRepository.findByUsername("usage_user_a").orElseThrow();
        User userB = userRepository.findByUsername("usage_user_b").orElseThrow();

        usageRepository.save(log(userA.getId(), "jobBrief", "gpt-a", 100, 30, 10, 0, 2, true, false));
        usageRepository.save(log(userA.getId(), "trainingFeedback", "gpt-a", 80, 20, 0, 5, 0, true, true));
        usageRepository.save(log(userB.getId(), "jobBrief", "gpt-b", 999, 999, 0, 0, 0, true, false));

        mockMvc.perform(get("/api/ai-usage/me/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(2))
                .andExpect(jsonPath("$.successfulRequests").value(2))
                .andExpect(jsonPath("$.failedRequests").value(0))
                .andExpect(jsonPath("$.estimatedRequests").value(1))
                .andExpect(jsonPath("$.totalInputTokens").value(180))
                .andExpect(jsonPath("$.totalOutputTokens").value(50))
                .andExpect(jsonPath("$.totalCacheReadTokens").value(10))
                .andExpect(jsonPath("$.totalCacheCreationTokens").value(5))
                .andExpect(jsonPath("$.totalReasoningTokens").value(2))
                .andExpect(jsonPath("$.totalTokens").value(247));

        mockMvc.perform(get("/api/ai-usage/me/summary")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(1))
                .andExpect(jsonPath("$.totalTokens").value(1998));
    }

    @Test
    void exposesDailyTaskAndModelBreakdownsForCurrentUser() throws Exception {
        String token = loginAndGetToken("usage_breakdown_user");
        User user = userRepository.findByUsername("usage_breakdown_user").orElseThrow();

        usageRepository.save(log(user.getId(), "jobBrief", "gpt-4o", 10, 5, 0, 0, 0, true, false));
        usageRepository.save(log(user.getId(), "jobBrief", "gpt-4o-mini", 20, 10, 0, 0, 0, true, false));
        usageRepository.save(log(user.getId(), "mockInterviewQuestion", "gpt-4o", 30, 15, 0, 0, 0, false, false));

        mockMvc.perform(get("/api/ai-usage/me/by-task")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='jobBrief')].totalRequests").value(2))
                .andExpect(jsonPath("$[?(@.name=='jobBrief')].totalTokens").value(45))
                .andExpect(jsonPath("$[?(@.name=='mockInterviewQuestion')].failedRequests").value(1));

        mockMvc.perform(get("/api/ai-usage/me/by-model")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='gpt-4o')].totalRequests").value(2))
                .andExpect(jsonPath("$[?(@.name=='gpt-4o-mini')].totalTokens").value(30));

        mockMvc.perform(get("/api/ai-usage/me/daily")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalRequests").value(3))
                .andExpect(jsonPath("$[0].totalTokens").value(90));
    }

    @Test
    void usageEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/ai-usage/me/summary"))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private AiUsageLog log(UUID userId,
                           String task,
                           String model,
                           int inputTokens,
                           int outputTokens,
                           int cacheReadTokens,
                           int cacheCreationTokens,
                           int reasoningTokens,
                           boolean success,
                           boolean estimated) {
        AiUsageLog log = new AiUsageLog();
        log.setUserId(userId);
        log.setTargetId(UUID.randomUUID());
        log.setRequestId(UUID.randomUUID().toString());
        log.setTask(task);
        log.setProviderType("platformDefault");
        log.setModel(model);
        log.setMode("chatCompletions");
        log.setUsageSource(estimated ? "estimatedFallback" : "openAiResponseUsage");
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setCacheReadTokens(cacheReadTokens);
        log.setCacheCreationTokens(cacheCreationTokens);
        log.setReasoningTokens(reasoningTokens);
        log.setTotalTokens(inputTokens + outputTokens + cacheReadTokens + cacheCreationTokens + reasoningTokens);
        log.setEstimated(estimated);
        log.setSuccess(success);
        log.setDurationMs(100);
        log.setCreatedAt(Instant.now());
        return log;
    }
}
