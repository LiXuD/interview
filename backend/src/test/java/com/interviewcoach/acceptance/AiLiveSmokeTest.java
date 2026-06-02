package com.interviewcoach.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.PlatformAiProperties;
import com.interviewcoach.common.api.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task 26/27: 真实 AI 冒烟测试 — 验证核心 AI task 的端到端链路可达性。
 *
 * 比 AiContentQualityTest 更轻量：只验证结构正确性，不断言内容质量。
 * 允许 AI 偶发解析失败（模型波动），只断言基础设施和链路可达。
 *
 * 运行方式：
 *   cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiLiveSmokeTest test
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("live-ai-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 300, unit = TimeUnit.SECONDS)
class AiLiveSmokeTest {

    private static final boolean LIVE_AI_ENABLED =
            "true".equalsIgnoreCase(System.getenv("IC_LIVE_AI_TEST"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformAiProperties platformAiProperties;

    @BeforeAll
    void checkLiveAiAvailable() {
        assumeTrue(LIVE_AI_ENABLED,
                "跳过真实 AI 冒烟测试：需设置 IC_LIVE_AI_TEST=true 启用");
        assumeTrue(platformAiProperties.isEnabled(),
                "跳过真实 AI 冒烟测试：平台 AI 未启用");
        assumeTrue(platformAiProperties.isComplete(),
                "跳过真实 AI 冒烟测试：平台 AI 配置不完整");
    }

    @Test
    @Order(1)
    @DisplayName("冒烟: 端到端链路可达性 — 基础设施 + AI task 成功率")
    void fullPipelineSmokeTest() throws Exception {
        // 1. 登录（非 AI，必须成功）
        String token = loginAndGetToken("smoke_user");

        // 2. 创建目标岗位（非 AI，必须成功）
        String targetId = createTarget(token, "Java Backend",
                "熟悉 Java/Spring Boot/MySQL/Redis");

        // 3. 确认候选人摘要（非 AI，必须成功）
        confirmProfile(token, targetId,
                "5年Java后端开发经验，熟悉Spring Boot和微服务架构",
                java.util.List.of("Java", "Spring Boot", "MySQL", "Redis"),
                java.util.List.of("支付系统重构，日均处理百万级交易"),
                java.util.List.of("某互联网公司 后端工程师 3年"));

        int aiTasksAttempted = 0;
        int aiTasksSucceeded = 0;

        // 4. JobBrief
        aiTasksAttempted++;
        MvcResult jobBriefResult = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andReturn();
        if (jobBriefResult.getResponse().getStatus() == 200) {
            JobBriefDto jobBrief = objectMapper.readValue(
                    jobBriefResult.getResponse().getContentAsString(), JobBriefDto.class);
            assertThat(jobBrief.roleSummary()).isNotBlank();
            aiTasksSucceeded++;
        }

        // 5. Assessment: start + 5 answers + finish
        aiTasksAttempted++;
        MvcResult startResult = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(targetId))))
                .andReturn();
        if (startResult.getResponse().getStatus() == 200) {
            aiTasksSucceeded++;
        }
        String sessionId = null;
        if (startResult.getResponse().getStatus() == 200) {
            sessionId = objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText();
        }

        int successfulScores = 0;
        if (sessionId != null) {
            for (int i = 0; i < 5; i++) {
                aiTasksAttempted++;
                MvcResult answerResult = mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new AssessmentAnswerRequest("回答第" + (i + 1) + "题"))))
                        .andReturn();
                if (answerResult.getResponse().getStatus() == 200) {
                    aiTasksSucceeded++;
                    successfulScores++;
                }
            }

            aiTasksAttempted++;
            MvcResult finishResult = mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
            if (finishResult.getResponse().getStatus() == 200) {
                aiTasksSucceeded++;
            }
        }

        // 6. TrainingPlan
        aiTasksAttempted++;
        MvcResult planResult = mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingPlanGenerateRequest(targetId))))
                .andReturn();
        String taskId = null;
        if (planResult.getResponse().getStatus() == 200) {
            aiTasksSucceeded++;
            taskId = objectMapper.readTree(planResult.getResponse().getContentAsString())
                    .get("tasks").get(0).get("id").asText();
        }

        // 7. TrainingFeedback
        if (taskId != null) {
            aiTasksAttempted++;
            MvcResult feedbackResult = mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new TrainingTaskAnswerRequest("使用Spring Boot自动配置简化开发"))))
                    .andReturn();
            if (feedbackResult.getResponse().getStatus() == 200) {
                aiTasksSucceeded++;
            }
        }

        // 8. MockInterview: start + 1 answer + finish
        aiTasksAttempted++;
        MvcResult interviewResult = mockMvc.perform(post("/api/mock-interviews/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewStartRequest(targetId, null))))
                .andReturn();
        String interviewId = null;
        if (interviewResult.getResponse().getStatus() == 200) {
            aiTasksSucceeded++;
            interviewId = objectMapper.readTree(interviewResult.getResponse().getContentAsString()).get("id").asText();
        }

        if (interviewId != null) {
            aiTasksAttempted++;
            MvcResult answerResult = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new MockInterviewAnswerRequest("我使用Redis缓存热点数据"))))
                    .andReturn();
            if (answerResult.getResponse().getStatus() == 200) {
                aiTasksSucceeded++;
            }

            aiTasksAttempted++;
            MvcResult reportResult = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
            if (reportResult.getResponse().getStatus() == 200) {
                aiTasksSucceeded++;
            }
        }

        // 最终断言：至少 50% 的 AI task 成功（允许模型波动）
        double successRate = (double) aiTasksSucceeded / aiTasksAttempted;
        assertThat(successRate)
                .as("AI task 成功率 %.0f%% (%d/%d) — 允许模型偶发失败，但基础设施必须可达",
                        successRate * 100, aiTasksSucceeded, aiTasksAttempted)
                .isGreaterThanOrEqualTo(0.5);
    }

    private String loginAndGetToken(String username) throws Exception {
        var request = new LoginRequest(username);
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

    private void confirmProfile(String token, String targetId, String summary,
                                java.util.List<String> skills, java.util.List<String> projects,
                                java.util.List<String> experience) throws Exception {
        var request = new CandidateProfileConfirmRequest(targetId, summary, skills, projects, experience);
        mockMvc.perform(post("/api/profiles/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
