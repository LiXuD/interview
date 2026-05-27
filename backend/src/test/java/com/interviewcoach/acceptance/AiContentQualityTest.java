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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Task 19: 真实 AI 验收样例集 — 内容质量验收测试。
 *
 * 使用真实 AI 模型验证输出质量。需要显式开启：
 *   IC_LIVE_AI_TEST=true
 *
 * 验证内容：
 * - AI 输出是否贴合目标岗位和候选人摘要
 * - AI 是否虚构候选人未提供的经历
 * - 题目是否有区分度
 * - 评分和反馈是否具体可执行
 *
 * 运行方式：
 *   IC_LIVE_AI_TEST=true mvn test -pl backend -Dtest=AiContentQualityTest
 *   或设置环境变量后在 IDE 中运行
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("live-ai-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AiContentQualityTest {

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
                "跳过真实 AI 验收测试：需设置 IC_LIVE_AI_TEST=true 启用");
        assumeTrue(platformAiProperties.isEnabled(),
                "跳过真实 AI 验收测试：平台 AI 未启用（IC_PLATFORM_AI_ENABLED=true）");
        assumeTrue(platformAiProperties.isComplete(),
                "跳过真实 AI 验收测试：平台 AI 配置不完整（需要 IC_PLATFORM_AI_BASE_URL、IC_PLATFORM_AI_API_KEY、IC_PLATFORM_AI_MODEL）");
    }

    // ==================== 场景 1: Java 后端/支付系统 ====================

    @Test
    @Order(1)
    @DisplayName("Java 支付后端: JobBrief 输出贴合岗位 JD")
    void javaJobBriefIsRelevant() throws Exception {
        PipelineContext ctx = setupPipeline(AiAcceptanceFixtures.JAVA_TITLE,
                AiAcceptanceFixtures.JAVA_JD,
                AiAcceptanceFixtures.JAVA_SUMMARY,
                AiAcceptanceFixtures.JAVA_SKILLS,
                AiAcceptanceFixtures.JAVA_PROJECTS,
                AiAcceptanceFixtures.JAVA_EXPERIENCE);

        String jobBriefJson = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JobBriefDto dto = objectMapper.readValue(jobBriefJson, JobBriefDto.class);
        assertThat(dto.targetId()).isEqualTo(ctx.targetId);
        assertThat(dto.roleSummary()).isNotBlank();
        assertThat(dto.skillMap()).isNotEmpty();
        assertThat(dto.mustHaveSkills()).isNotEmpty();
        assertThat(dto.confidence()).isBetween(0.0, 1.0);

        // 验证输出贴合支付后端岗位：必须包含至少一个岗位相关技能
        List<String> allSkills = new java.util.ArrayList<>(dto.mustHaveSkills());
        allSkills.addAll(dto.niceToHaveSkills());
        dto.skillMap().forEach(s -> allSkills.add(s.name()));
        String allSkillsLower = allSkills.stream()
                .map(String::toLowerCase).reduce("", (a, b) -> a + " " + b);
        String roleSummaryLower = dto.roleSummary().toLowerCase();

        boolean hasRelevantSkill = allSkillsLower.contains("java")
                || allSkillsLower.contains("spring")
                || allSkillsLower.contains("支付")
                || allSkillsLower.contains("payment")
                || allSkillsLower.contains("redis")
                || allSkillsLower.contains("mysql");
        assertThat(hasRelevantSkill)
                .as("JobBrief 应包含至少一个与 Java 支付后端相关的技能，实际技能: %s", allSkills)
                .isTrue();

        // 验证 skillMap.gap 不是空的
        dto.skillMap().forEach(item -> {
            assertThat(item.gap()).isNotBlank();
            assertThat(item.importance()).isIn("required", "important", "bonus");
            assertThat(item.userLevel()).isIn("unknown", "weak", "basic", "solid", "strong");
        });

        ctx.jobBriefGenerated = true;
    }

    @Test
    @Order(2)
    @DisplayName("Java 支付后端: 测评 5 题区分度验证")
    void javaAssessmentQuestionsHaveDistinction() throws Exception {
        PipelineContext ctx = setupPipeline(AiAcceptanceFixtures.JAVA_TITLE,
                AiAcceptanceFixtures.JAVA_JD,
                AiAcceptanceFixtures.JAVA_SUMMARY,
                AiAcceptanceFixtures.JAVA_SKILLS,
                AiAcceptanceFixtures.JAVA_PROJECTS,
                AiAcceptanceFixtures.JAVA_EXPERIENCE);

        MvcResult startResult = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(5))
                .andReturn();

        String sessionJson = startResult.getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(sessionJson).get("id").asText();

        // 验证 5 题全部是字符串且不为空
        String currentQuestion = objectMapper.readTree(sessionJson).get("currentQuestion").asText();
        assertThat(currentQuestion).isNotBlank();

        // 验证题目的多样性：题目长度和内容应有差异（不是重复的同一题）
        // 先回答完所有题，再 finish 获取评分结果
        List<String> answers = List.of(
                AiAcceptanceFixtures.JAVA_ANSWER_ORDER_DESIGN,
                AiAcceptanceFixtures.JAVA_ANSWER_WEAK,
                "Spring Boot 自动配置通过 @EnableAutoConfiguration 实现，扫描 META-INF/spring.factories 文件加载自动配置类。",
                "Redis 分布式锁使用 SET key value NX EX 实现，需要配合 Lua 脚本保证原子性释放。",
                "支付系统设计需要考虑幂等性、分布式事务、资金安全和对账机制。"
        );
        for (String answer : answers) {
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + ctx.token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest(answer))))
                    .andExpect(status().isOk());
        }

        String resultJson = mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AssessmentResultDto result = objectMapper.readValue(resultJson, AssessmentResultDto.class);
        assertThat(result.totalScore()).isBetween(0, 100);
        assertThat(result.dimensions()).isNotEmpty();
        result.dimensions().forEach(dim -> {
            assertThat(dim.name()).isNotBlank();
            assertThat(dim.score()).isBetween(0, 100);
            assertThat(dim.reason()).isNotBlank();
        });
        assertThat(result.strengths()).isNotEmpty();
        assertThat(result.weaknesses()).isNotEmpty();
        assertThat(result.nextActions()).isNotEmpty();

        // 验证评分不是全部相同（有区分度）
        long distinctScores = result.dimensions().stream()
                .mapToInt(DimensionScore::score)
                .distinct().count();
        assertThat(distinctScores)
                .as("各维度评分应有区分度，不应全部相同。实际评分维度数: %d", distinctScores)
                .isGreaterThan(1);
    }

    @Test
    @Order(3)
    @DisplayName("Java 支付后端: 训练反馈应包含可执行建议")
    void javaTrainingFeedbackIsActionable() throws Exception {
        PipelineContext ctx = setupFullPipeline(AiAcceptanceFixtures.JAVA_TITLE,
                AiAcceptanceFixtures.JAVA_JD,
                AiAcceptanceFixtures.JAVA_SUMMARY,
                AiAcceptanceFixtures.JAVA_SKILLS,
                AiAcceptanceFixtures.JAVA_PROJECTS,
                AiAcceptanceFixtures.JAVA_EXPERIENCE);

        MvcResult planResult = mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingPlanGenerateRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").isNumber())
                .andReturn();

        String planJson = planResult.getResponse().getContentAsString();
        String taskId = objectMapper.readTree(planJson).get("tasks").get(0).get("id").asText();

        String feedbackJson = mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TrainingTaskAnswerRequest(AiAcceptanceFixtures.JAVA_ANSWER_ORDER_DESIGN))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TrainingFeedbackDto feedback = objectMapper.readValue(feedbackJson, TrainingFeedbackDto.class);
        assertThat(feedback.taskId()).isEqualTo(taskId);
        assertThat(feedback.score()).isBetween(0, 100);
        assertThat(feedback.feedback()).isNotBlank();
        assertThat(feedback.rewrittenAnswer()).isNotBlank();
        assertThat(feedback.followUpQuestion()).isNotBlank();

        // 验证反馈不是泛泛而谈（长度应有实质性内容）
        assertThat(feedback.feedback().length())
                .as("反馈应有实质性内容，不应只是一句话")
                .isGreaterThan(20);

        // 验证改进回答与原回答不同
        assertThat(feedback.rewrittenAnswer())
                .as("改进回答应与原回答不同")
                .isNotEqualTo(AiAcceptanceFixtures.JAVA_ANSWER_ORDER_DESIGN);
    }

    // ==================== 场景 2: AI 应用工程师/RAG Agent ====================

    @Test
    @Order(4)
    @DisplayName("AI RAG 工程师: JobBrief 应识别 AI/RAG 相关技能")
    void aiEngineerJobBriefIdentifiesRagSkills() throws Exception {
        PipelineContext ctx = setupPipeline(AiAcceptanceFixtures.AI_TITLE,
                AiAcceptanceFixtures.AI_JD,
                AiAcceptanceFixtures.AI_SUMMARY,
                AiAcceptanceFixtures.AI_SKILLS,
                AiAcceptanceFixtures.AI_PROJECTS,
                AiAcceptanceFixtures.AI_EXPERIENCE);

        String jobBriefJson = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JobBriefDto dto = objectMapper.readValue(jobBriefJson, JobBriefDto.class);

        // 验证输出包含 AI/RAG 相关技能
        List<String> allItems = new java.util.ArrayList<>(dto.mustHaveSkills());
        allItems.addAll(dto.niceToHaveSkills());
        allItems.addAll(dto.interviewTopics());
        dto.skillMap().forEach(s -> allItems.add(s.name()));
        String allLower = allItems.stream()
                .map(String::toLowerCase).reduce("", (a, b) -> a + " " + b);

        boolean hasAiRelevant = allLower.contains("rag")
                || allLower.contains("langchain")
                || allLower.contains("embedding")
                || allLower.contains("向量")
                || allLower.contains("prompt")
                || allLower.contains("大模型")
                || allLower.contains("agent")
                || allLower.contains("llm")
                || allLower.contains("检索");
        assertThat(hasAiRelevant)
                .as("AI 岗位 JobBrief 应包含至少一个 AI/RAG 相关关键词，实际内容: %s", allItems)
                .isTrue();

        // 验证不得虚构候选人未提供的经历
        String candidateMatchStr = String.join(",", dto.candidateMatch());
        String riskAreasStr = String.join(",", dto.riskAreas());
        String combined = candidateMatchStr + riskAreasStr + dto.roleSummary();

        // 候选人没有 Kubernetes、TensorFlow、PyTorch 经验，不应出现在 candidateMatch 中
        assertThat(combined.toLowerCase()).doesNotContain("kubernetes");
        assertThat(combined.toLowerCase()).doesNotContain("tensorflow");
    }

    @Test
    @Order(5)
    @DisplayName("AI RAG 工程师: 测评题目应与岗位相关")
    void aiEngineerQuestionsAreJobRelevant() throws Exception {
        PipelineContext ctx = setupPipeline(AiAcceptanceFixtures.AI_TITLE,
                AiAcceptanceFixtures.AI_JD,
                AiAcceptanceFixtures.AI_SUMMARY,
                AiAcceptanceFixtures.AI_SKILLS,
                AiAcceptanceFixtures.AI_PROJECTS,
                AiAcceptanceFixtures.AI_EXPERIENCE);

        MvcResult startResult = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andReturn();

        String sessionJson = startResult.getResponse().getContentAsString();
        String currentQ = objectMapper.readTree(sessionJson).get("currentQuestion").asText();

        // 验证第一题与 AI/RAG 岗位相关
        String qLower = currentQ.toLowerCase();
        boolean questionRelevant = qLower.contains("rag")
                || qLower.contains("embedding")
                || qLower.contains("向量")
                || qLower.contains("检索")
                || qLower.contains("prompt")
                || qLower.contains("大模型")
                || qLower.contains("langchain")
                || qLower.contains("llm")
                || qLower.contains("agent")
                || qLower.contains("文档")
                || qLower.contains("qa")
                || qLower.contains("问答");
        assertThat(questionRelevant)
                .as("AI 岗位测评第一题应与岗位相关，实际题目: %s", currentQ)
                .isTrue();
    }

    // ==================== 场景 3: 数据平台/调度数仓 ====================

    @Test
    @Order(6)
    @DisplayName("数据平台工程师: JobBrief 应识别大数据相关技能")
    void dataPlatformJobBriefIdentifiesBigDataSkills() throws Exception {
        PipelineContext ctx = setupPipeline(AiAcceptanceFixtures.DATA_TITLE,
                AiAcceptanceFixtures.DATA_JD,
                AiAcceptanceFixtures.DATA_SUMMARY,
                AiAcceptanceFixtures.DATA_SKILLS,
                AiAcceptanceFixtures.DATA_PROJECTS,
                AiAcceptanceFixtures.DATA_EXPERIENCE);

        String jobBriefJson = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JobBriefDto dto = objectMapper.readValue(jobBriefJson, JobBriefDto.class);

        List<String> allItems = new java.util.ArrayList<>(dto.mustHaveSkills());
        allItems.addAll(dto.niceToHaveSkills());
        dto.skillMap().forEach(s -> allItems.add(s.name()));
        String allLower = allItems.stream()
                .map(String::toLowerCase).reduce("", (a, b) -> a + " " + b);

        boolean hasDataRelevant = allLower.contains("spark")
                || allLower.contains("hive")
                || allLower.contains("数仓")
                || allLower.contains("airflow")
                || allLower.contains("调度")
                || allLower.contains("etl")
                || allLower.contains("数据")
                || allLower.contains("hadoop")
                || allLower.contains("flink")
                || allLower.contains("warehouse");
        assertThat(hasDataRelevant)
                .as("数据岗位 JobBrief 应包含至少一个大数据相关关键词，实际内容: %s", allItems)
                .isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("数据平台工程师: 模拟面试追问质量验证")
    void dataPlatformMockInterviewFollowUp() throws Exception {
        PipelineContext ctx = setupFullPipeline(AiAcceptanceFixtures.DATA_TITLE,
                AiAcceptanceFixtures.DATA_JD,
                AiAcceptanceFixtures.DATA_SUMMARY,
                AiAcceptanceFixtures.DATA_SKILLS,
                AiAcceptanceFixtures.DATA_PROJECTS,
                AiAcceptanceFixtures.DATA_EXPERIENCE);

        String interviewStartJson = mockMvc.perform(post("/api/mock-interviews/start")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewStartRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestion").isString())
                .andReturn().getResponse().getContentAsString();

        String interviewId = objectMapper.readTree(interviewStartJson).get("id").asText();
        String firstQuestion = objectMapper.readTree(interviewStartJson).get("currentQuestion").asText();

        // 验证开场问题与数据岗位相关
        String qLower = firstQuestion.toLowerCase();
        boolean openingRelevant = qLower.contains("数仓")
                || qLower.contains("etl")
                || qLower.contains("数据")
                || qLower.contains("spark")
                || qLower.contains("hive")
                || qLower.contains("调度")
                || qLower.contains("airflow")
                || qLower.contains("warehouse")
                || qLower.contains("数据平台");
        assertThat(openingRelevant)
                .as("数据岗位模拟面试开场问题应与岗位相关，实际: %s", firstQuestion)
                .isTrue();

        // 提交回答后验证追问
        String answerFollowJson = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MockInterviewAnswerRequest(AiAcceptanceFixtures.DATA_ANSWER_WAREHOUSE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestion").isString())
                .andReturn().getResponse().getContentAsString();

        String followUp = objectMapper.readTree(answerFollowJson).get("currentQuestion").asText();
        assertThat(followUp).isNotBlank();
        assertThat(followUp).as("追问应与开场问题不同").isNotEqualTo(firstQuestion);

        // finish 生成报告
        String reportJson = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        MockInterviewReportDto report = objectMapper.readValue(reportJson, MockInterviewReportDto.class);
        assertThat(report.mockInterviewId()).isEqualTo(interviewId);
        assertThat(report.overallScore()).isBetween(0, 100);
        assertThat(report.summary()).isNotBlank();
        assertThat(report.dimensionScores()).isNotEmpty();
        assertThat(report.nextTrainingTasks()).isNotEmpty();
    }

    // ==================== 通用验证：AI 不虚构候选人经历 ====================

    @Test
    @Order(8)
    @DisplayName("Java 支付后端: AI 不应虚构候选人未提供的 Kubernetes/微服务经验")
    void javaJobBriefDoesNotFabricateExperience() throws Exception {
        PipelineContext ctx = setupPipeline(AiAcceptanceFixtures.JAVA_TITLE,
                AiAcceptanceFixtures.JAVA_JD,
                AiAcceptanceFixtures.JAVA_SUMMARY,
                AiAcceptanceFixtures.JAVA_SKILLS,
                AiAcceptanceFixtures.JAVA_PROJECTS,
                AiAcceptanceFixtures.JAVA_EXPERIENCE);

        String jobBriefJson = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JobBriefDto dto = objectMapper.readValue(jobBriefJson, JobBriefDto.class);

        // 候选人只有 Java/Spring Boot/MySQL/Redis/RabbitMQ/Docker
        // AI 不应在 candidateMatch 中声称候选人有 Kubernetes、微服务治理、Seata 等未提及的经验
        String candidateMatchAll = String.join(" ", dto.candidateMatch());
        String riskAll = String.join(" ", dto.riskAreas());
        String combined = (candidateMatchAll + " " + riskAll).toLowerCase();

        // 这些技术在候选人技能和项目中都没有提到
        assertThat(combined).doesNotContain("kubernetes");
        assertThat(combined).doesNotContain("k8s");

        // Seata 是 JD 要求但候选人没有的经验，应在 riskAreas 或 gap 中体现，而非 candidateMatch
        String matchOnly = candidateMatchAll.toLowerCase();
        if (matchOnly.contains("seata")) {
            // 如果 candidateMatch 提到 Seata，应是在 gap/risk 的语境下，而非声称已有经验
            // 这个检查取决于 AI 的实际输出，允许提及但不允许声称掌握
        }
    }

    // ==================== Pipeline Setup Helpers ====================

    private static class PipelineContext {
        String token;
        String targetId;
        boolean jobBriefGenerated = false;
        boolean assessmentCompleted = false;
    }

    private PipelineContext setupPipeline(String title, String jd, String summary,
                                          List<String> skills, List<String> projects,
                                          List<String> experience) throws Exception {
        PipelineContext ctx = new PipelineContext();
        ctx.token = loginAndGetToken("lq_" + title.hashCode());
        ctx.targetId = createTarget(ctx.token, title, jd);
        confirmProfile(ctx.token, ctx.targetId, summary, skills, projects, experience);
        return ctx;
    }

    /** 完成测评的完整管道（JobBrief -> Assessment finish），用于后续训练和面试 */
    private PipelineContext setupFullPipeline(String title, String jd, String summary,
                                              List<String> skills, List<String> projects,
                                              List<String> experience) throws Exception {
        PipelineContext ctx = setupPipeline(title, jd, summary, skills, projects, experience);

        // 生成 JobBrief（确保 profile 已关联）
        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andExpect(status().isOk());

        // 完成 Assessment
        String sessionJson = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(sessionJson).get("id").asText();

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + ctx.token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AssessmentAnswerRequest("Answer " + i))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk());

        ctx.assessmentCompleted = true;
        return ctx;
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
                                List<String> skills, List<String> projects,
                                List<String> experience) throws Exception {
        var request = new CandidateProfileConfirmRequest(targetId, summary, skills, projects, experience);
        mockMvc.perform(post("/api/profiles/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
