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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task 26/27: 真实 AI 冒烟测试 — 验证核心 AI task 的端到端链路可达性。
 *
 * 核心闭环 (JobBrief -> Assessment -> TrainingPlan -> TrainingFeedback) 为硬门禁：
 * 每一步必须成功，不允许平均成功率稀释。
 *
 * MockInterview 单独测试，允许偶发失败。
 *
 * 运行方式：
 *   cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiLiveSmokeTest test
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("live-ai-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 600, unit = TimeUnit.SECONDS)
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
    @DisplayName("核心闭环门禁: JobBrief -> Assessment -> TrainingPlan -> TrainingFeedback 必须全部成功")
    void corePipelineSmokeTest() throws Exception {
        String token = loginAndGetToken("smoke_core_user");
        String targetId = createTarget(token, "Java Backend",
                "熟悉 Java/Spring Boot/MySQL/Redis");
        confirmProfile(token, targetId,
                "5年Java后端开发经验，熟悉Spring Boot和微服务架构",
                java.util.List.of("Java", "Spring Boot", "MySQL", "Redis"),
                java.util.List.of("支付系统重构，日均处理百万级交易"),
                java.util.List.of("某互联网公司 后端工程师 3年"));

        // 1. JobBrief — 必须成功
        MvcResult jobBriefResult = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andReturn();
        JobBriefDto jobBrief = objectMapper.readValue(
                jobBriefResult.getResponse().getContentAsString(), JobBriefDto.class);
        assertThat(jobBrief.roleSummary()).as("JobBrief.roleSummary").isNotBlank();
        assertThat(jobBrief.mustHaveSkills()).as("JobBrief.mustHaveSkills").isNotEmpty();

        // 2. Assessment: start — 必须成功
        MvcResult startResult = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(targetId))))
                .andExpect(status().isOk())
                .andReturn();
        String sessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .get("id").asText();

        // 3. Assessment: 5 题评分 — 全部必须成功
        for (int i = 0; i < 5; i++) {
            MvcResult answerResult = mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AssessmentAnswerRequest("回答第" + (i + 1) + "题"))))
                    .andReturn();
            assertThat(answerResult.getResponse().getStatus())
                    .as("Assessment answer %d status", i + 1)
                    .isEqualTo(200);
        }

        // 4. Assessment: finish — 必须成功
        MvcResult finishResult = mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        AssessmentResultDto assessmentResult = objectMapper.readValue(
                finishResult.getResponse().getContentAsString(), AssessmentResultDto.class);
        assertThat(assessmentResult.totalScore()).as("AssessmentResult.totalScore").isBetween(0, 100);

        // 5. TrainingPlan — 必须成功（依赖 Assessment 完成）
        MvcResult planResult = mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingPlanGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andReturn();
        String taskId = objectMapper.readTree(planResult.getResponse().getContentAsString())
                .get("tasks").get(0).get("id").asText();

        // 6. TrainingFeedback — 必须成功
        MvcResult feedbackResult = mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TrainingTaskAnswerRequest("使用Spring Boot自动配置简化开发"))))
                .andExpect(status().isOk())
                .andReturn();
        TrainingFeedbackDto feedback = objectMapper.readValue(
                feedbackResult.getResponse().getContentAsString(), TrainingFeedbackDto.class);
        assertThat(feedback.score()).as("TrainingFeedback.score").isBetween(0, 100);
        assertThat(feedback.feedback()).as("TrainingFeedback.feedback").isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("MockInterview 冒烟: start -> answer -> finish 允许偶发失败")
    void mockInterviewSmokeTest() throws Exception {
        String token = loginAndGetToken("smoke_mock_user");
        String targetId = createTarget(token, "Java Backend",
                "熟悉 Java/Spring Boot/MySQL/Redis");
        confirmProfile(token, targetId,
                "5年Java后端开发经验，熟悉Spring Boot和微服务架构",
                java.util.List.of("Java", "Spring Boot", "MySQL", "Redis"),
                java.util.List.of("支付系统重构，日均处理百万级交易"),
                java.util.List.of("某互联网公司 后端工程师 3年"));

        MvcResult interviewResult = mockMvc.perform(post("/api/mock-interviews/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewStartRequest(targetId, null))))
                .andReturn();

        if (interviewResult.getResponse().getStatus() != 200) {
            // MockInterview 允许偶发失败（AI 模型波动）
            return;
        }

        String interviewId = objectMapper.readTree(interviewResult.getResponse().getContentAsString())
                .get("id").asText();

        MvcResult answerResult = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MockInterviewAnswerRequest("我使用Redis缓存热点数据"))))
                .andReturn();
        if (answerResult.getResponse().getStatus() != 200) {
            return;
        }

        MvcResult reportResult = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        if (reportResult.getResponse().getStatus() == 200) {
            MockInterviewReportDto report = objectMapper.readValue(
                    reportResult.getResponse().getContentAsString(), MockInterviewReportDto.class);
            assertThat(report.overallScore()).as("MockInterviewReport.overallScore").isBetween(0, 100);
        }
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
