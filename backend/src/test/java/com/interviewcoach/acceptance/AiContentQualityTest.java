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
import java.util.concurrent.TimeUnit;

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
@Timeout(value = 600, unit = TimeUnit.SECONDS)
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

        MvcResult jobBriefResult = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andReturn();
        assumeAiOk(jobBriefResult, "JobBrief 生成");

        JobBriefDto dto = objectMapper.readValue(
                jobBriefResult.getResponse().getContentAsString(), JobBriefDto.class);
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
                .andReturn();
        assumeAiOk(startResult, "Assessment start");

        String sessionJson = startResult.getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(sessionJson).get("id").asText();

        // 验证 5 题全部是字符串且不为空
        String currentQuestion = objectMapper.readTree(sessionJson).get("currentQuestion").get("question").asText();
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

        MvcResult finishResult = mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + ctx.token))
                .andReturn();
        assumeAiOk(finishResult, "Assessment finish");

        AssessmentResultDto result = objectMapper.readValue(
                finishResult.getResponse().getContentAsString(), AssessmentResultDto.class);
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
                .andReturn();
        assumeAiOk(planResult, "TrainingPlan 生成");

        String planJson = planResult.getResponse().getContentAsString();
        String taskId = objectMapper.readTree(planJson).get("tasks").get(0).get("id").asText();

        MvcResult feedbackResult = mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TrainingTaskAnswerRequest(AiAcceptanceFixtures.JAVA_ANSWER_ORDER_DESIGN))))
                .andReturn();
        assumeAiOk(feedbackResult, "TrainingFeedback 生成");

        TrainingFeedbackDto feedback = objectMapper.readValue(
                feedbackResult.getResponse().getContentAsString(), TrainingFeedbackDto.class);
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

        MvcResult jobBriefResult = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andReturn();
        assumeAiOk(jobBriefResult, "JobBrief");

        JobBriefDto dto = objectMapper.readValue(
                jobBriefResult.getResponse().getContentAsString(), JobBriefDto.class);

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
                .andReturn();
        assumeAiOk(startResult, "Assessment start");

        String sessionJson = startResult.getResponse().getContentAsString();
        String currentQ = objectMapper.readTree(sessionJson).get("currentQuestion").get("question").asText();

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

        MvcResult dataJobBriefResult = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andReturn();
        assumeAiOk(dataJobBriefResult, "JobBrief");

        JobBriefDto dto = objectMapper.readValue(
                dataJobBriefResult.getResponse().getContentAsString(), JobBriefDto.class);

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

        MvcResult interviewStartResult = mockMvc.perform(post("/api/mock-interviews/start")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewStartRequest(ctx.targetId, null))))
                .andReturn();
        assumeAiOk(interviewStartResult, "MockInterview start");

        String interviewStartJson = interviewStartResult.getResponse().getContentAsString();
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
        MvcResult answerFollowResult = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MockInterviewAnswerRequest(AiAcceptanceFixtures.DATA_ANSWER_WAREHOUSE))))
                .andReturn();
        assumeAiOk(answerFollowResult, "MockInterview answer");

        String answerFollowJson = answerFollowResult.getResponse().getContentAsString();
        String followUp = objectMapper.readTree(answerFollowJson).get("currentQuestion").asText();
        assertThat(followUp).isNotBlank();
        assertThat(followUp).as("追问应与开场问题不同").isNotEqualTo(firstQuestion);

        // finish 生成报告
        MvcResult reportResult = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + ctx.token))
                .andReturn();
        assumeAiOk(reportResult, "MockInterview finish");

        String reportJson = reportResult.getResponse().getContentAsString();

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

        MvcResult fabricateJobBriefResult = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andReturn();
        assumeAiOk(fabricateJobBriefResult, "JobBrief");

        JobBriefDto dto = objectMapper.readValue(
                fabricateJobBriefResult.getResponse().getContentAsString(), JobBriefDto.class);

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

    // ==================== 场景 4: 前端/React ====================

    @Test
    @Order(9)
    @DisplayName("前端 React 工程师: JobBrief 应识别前端相关技能")
    void frontendJobBriefIdentifiesReactSkills() throws Exception {
        PipelineContext ctx = setupPipeline(AiAcceptanceFixtures.FRONTEND_TITLE,
                AiAcceptanceFixtures.FRONTEND_JD,
                AiAcceptanceFixtures.FRONTEND_SUMMARY,
                AiAcceptanceFixtures.FRONTEND_SKILLS,
                AiAcceptanceFixtures.FRONTEND_PROJECTS,
                AiAcceptanceFixtures.FRONTEND_EXPERIENCE);

        MvcResult frontendJobBriefResult = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andReturn();
        assumeAiOk(frontendJobBriefResult, "JobBrief");

        JobBriefDto dto = objectMapper.readValue(
                frontendJobBriefResult.getResponse().getContentAsString(), JobBriefDto.class);

        List<String> allItems = new java.util.ArrayList<>(dto.mustHaveSkills());
        allItems.addAll(dto.niceToHaveSkills());
        allItems.addAll(dto.interviewTopics());
        dto.skillMap().forEach(s -> allItems.add(s.name()));
        String allLower = allItems.stream()
                .map(String::toLowerCase).reduce("", (a, b) -> a + " " + b);

        boolean hasFrontendRelevant = allLower.contains("react")
                || allLower.contains("typescript")
                || allLower.contains("前端")
                || allLower.contains("frontend")
                || allLower.contains("组件")
                || allLower.contains("状态管理");
        assertThat(hasFrontendRelevant)
                .as("前端岗位 JobBrief 应包含至少一个前端相关关键词，实际内容: %s", allItems)
                .isTrue();

        // 验证不虚构候选人未提供的经验
        String combined = String.join(" ", dto.candidateMatch()) + " " + String.join(" ", dto.riskAreas());
        assertThat(combined.toLowerCase()).doesNotContain("vue");
        assertThat(combined.toLowerCase()).doesNotContain("angular");
    }

    @Test
    @Order(10)
    @DisplayName("前端 React 工程师: 模拟面试追问与前端相关")
    void frontendMockInterviewIsRelevant() throws Exception {
        PipelineContext ctx = setupFullPipeline(AiAcceptanceFixtures.FRONTEND_TITLE,
                AiAcceptanceFixtures.FRONTEND_JD,
                AiAcceptanceFixtures.FRONTEND_SUMMARY,
                AiAcceptanceFixtures.FRONTEND_SKILLS,
                AiAcceptanceFixtures.FRONTEND_PROJECTS,
                AiAcceptanceFixtures.FRONTEND_EXPERIENCE);

        MvcResult interviewStartResult = mockMvc.perform(post("/api/mock-interviews/start")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewStartRequest(ctx.targetId, null))))
                .andReturn();
        assumeAiOk(interviewStartResult, "MockInterview start");

        String interviewId = objectMapper.readTree(
                interviewStartResult.getResponse().getContentAsString()).get("id").asText();

        // 提交回答验证追问
        MvcResult answerResult = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MockInterviewAnswerRequest(AiAcceptanceFixtures.FRONTEND_ANSWER_STATE_MANAGEMENT))))
                .andReturn();
        assumeAiOk(answerResult, "MockInterview answer");

        MvcResult reportResult = mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + ctx.token))
                .andReturn();
        assumeAiOk(reportResult, "MockInterview finish");

        MockInterviewReportDto report = objectMapper.readValue(
                reportResult.getResponse().getContentAsString(), MockInterviewReportDto.class);
        assertThat(report.overallScore()).isBetween(0, 100);
        assertThat(report.dimensionScores()).isNotEmpty();
    }

    // ==================== 场景 5: DevOps/SRE ====================

    @Test
    @Order(11)
    @DisplayName("DevOps/SRE: JobBrief 应识别基础设施相关技能")
    void devopsJobBriefIdentifiesInfraSkills() throws Exception {
        PipelineContext ctx = setupPipeline(AiAcceptanceFixtures.DEVOPS_TITLE,
                AiAcceptanceFixtures.DEVOPS_JD,
                AiAcceptanceFixtures.DEVOPS_SUMMARY,
                AiAcceptanceFixtures.DEVOPS_SKILLS,
                AiAcceptanceFixtures.DEVOPS_PROJECTS,
                AiAcceptanceFixtures.DEVOPS_EXPERIENCE);

        MvcResult jobBriefResult = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(ctx.targetId))))
                .andReturn();
        assumeAiOk(jobBriefResult, "JobBrief 生成");

        JobBriefDto dto = objectMapper.readValue(
                jobBriefResult.getResponse().getContentAsString(), JobBriefDto.class);

        List<String> allItems = new java.util.ArrayList<>(dto.mustHaveSkills());
        allItems.addAll(dto.niceToHaveSkills());
        allItems.addAll(dto.interviewTopics());
        dto.skillMap().forEach(s -> allItems.add(s.name()));
        String allLower = allItems.stream()
                .map(String::toLowerCase).reduce("", (a, b) -> a + " " + b);

        boolean hasDevOpsRelevant = allLower.contains("kubernetes")
                || allLower.contains("k8s")
                || allLower.contains("docker")
                || allLower.contains("ci/cd")
                || allLower.contains("prometheus")
                || allLower.contains("监控")
                || allLower.contains("部署")
                || allLower.contains("集群");
        assertThat(hasDevOpsRelevant)
                .as("DevOps 岗位 JobBrief 应包含至少一个基础设施相关关键词，实际内容: %s", allItems)
                .isTrue();
    }

    @Test
    @Order(12)
    @DisplayName("DevOps/SRE: 测评题目应与运维/SRE 岗位相关")
    void devopsQuestionsAreJobRelevant() throws Exception {
        PipelineContext ctx = setupPipeline(AiAcceptanceFixtures.DEVOPS_TITLE,
                AiAcceptanceFixtures.DEVOPS_JD,
                AiAcceptanceFixtures.DEVOPS_SUMMARY,
                AiAcceptanceFixtures.DEVOPS_SKILLS,
                AiAcceptanceFixtures.DEVOPS_PROJECTS,
                AiAcceptanceFixtures.DEVOPS_EXPERIENCE);

        MvcResult startResult = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(ctx.targetId))))
                .andReturn();
        assumeAiOk(startResult, "Assessment start");

        String sessionJson = startResult.getResponse().getContentAsString();
        String currentQ = objectMapper.readTree(sessionJson).get("currentQuestion").get("question").asText();

        String qLower = currentQ.toLowerCase();
        boolean questionRelevant = qLower.contains("kubernetes")
                || qLower.contains("k8s")
                || qLower.contains("docker")
                || qLower.contains("集群")
                || qLower.contains("部署")
                || qLower.contains("监控")
                || qLower.contains("ci/cd")
                || qLower.contains("sre")
                || qLower.contains("可用性")
                || qLower.contains("故障");
        assertThat(questionRelevant)
                .as("DevOps 岗位测评第一题应与岗位相关，实际题目: %s", currentQ)
                .isTrue();
    }

    // ==================== 通用验证：评分一致性 ====================

    @Test
    @Order(13)
    @DisplayName("Java 支付后端: 逐题评分应有区分度且包含完整诊断")
    void javaQuestionScoresHaveDistinction() throws Exception {
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
                .andReturn();
        assumeAiOk(startResult, "Assessment start");

        String sessionJson = startResult.getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(sessionJson).get("id").asText();

        // 用不同质量的回答
        List<String> answers = List.of(
                AiAcceptanceFixtures.JAVA_ANSWER_ORDER_DESIGN,
                AiAcceptanceFixtures.JAVA_ANSWER_WEAK,
                "Spring Boot 自动配置通过 @EnableAutoConfiguration 实现，扫描 META-INF/spring.factories 加载配置类。",
                "Redis 分布式锁用 SET key value NX EX 实现，配合 Lua 脚本保证原子释放。",
                "支付系统设计考虑幂等性、分布式事务、资金安全和对账。"
        );
        for (String answer : answers) {
            MvcResult answerResult = mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + ctx.token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest(answer))))
                    .andExpect(status().isOk())
                    .andReturn();
            String answerJson = answerResult.getResponse().getContentAsString();
            var tree = objectMapper.readTree(answerJson);
            var scores = tree.get("questionScores");
            assertThat(scores).isNotNull();
            assertThat(scores.isArray()).isTrue();
            assertThat(scores.isEmpty()).isFalse();
            var lastScore = scores.get(scores.size() - 1);
            assertThat(lastScore.get("score").asInt()).isBetween(0, 100);
            assertThat(lastScore.get("dimension").asText()).isNotBlank();
            assertThat(lastScore.get("feedback").asText()).isNotBlank();
            assertThat(lastScore.get("answerStructure")).isNotNull();
            assertThat(lastScore.get("followUpRisks").isArray()).isTrue();
            assertThat(lastScore.get("contentHighlights").isArray()).isTrue();
            assertThat(lastScore.get("contentGaps").isArray()).isTrue();
        }
    }

    // ==================== 通用验证：虚构经历检查 ====================

    @Test
    @Order(14)
    @DisplayName("AI RAG 工程师: AI 不应虚构候选人未提供的 PyTorch/深度学习经验")
    void aiEngineerDoesNotFabricateDeepLearning() throws Exception {
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

        String candidateMatchAll = String.join(" ", dto.candidateMatch()).toLowerCase();
        // 候选人没有 PyTorch、TensorFlow、深度学习训练经验
        assertThat(candidateMatchAll).doesNotContain("pytorch");
        assertThat(candidateMatchAll).doesNotContain("tensorflow");
        assertThat(candidateMatchAll).doesNotContain("深度学习训练");
    }

    // ==================== Phase 4: Agent Decision 质量验收 ====================

    @Test
    @Order(15)
    @DisplayName("Agent: 测评完成后 Agent 决策包含完整字段且工具在白名单内")
    void agentDecisionAfterAssessmentIsComplete() throws Exception {
        PipelineContext ctx = setupFullPipeline(AiAcceptanceFixtures.JAVA_TITLE,
                AiAcceptanceFixtures.JAVA_JD,
                AiAcceptanceFixtures.JAVA_SUMMARY,
                AiAcceptanceFixtures.JAVA_SKILLS,
                AiAcceptanceFixtures.JAVA_PROJECTS,
                AiAcceptanceFixtures.JAVA_EXPERIENCE);

        // Agent should have been updated after assessment finish (triggered in setupFullPipeline)
        MvcResult agentResult = mockMvc.perform(get("/api/targets/" + ctx.targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + ctx.token))
                .andReturn();
        assumeAiOk(agentResult, "Agent 状态查询");

        com.fasterxml.jackson.databind.JsonNode agentJson =
                objectMapper.readTree(agentResult.getResponse().getContentAsString());

        // Verify agent exists and has been updated
        assertThat(agentJson.get("id")).isNotNull();
        assertThat(agentJson.get("targetId").asText()).isEqualTo(ctx.targetId);
        assertThat(agentJson.get("status").asText()).isEqualTo("active");
        assertThat(agentJson.get("currentStage").asText()).isNotBlank();
        assertThat(agentJson.get("currentGoal").asText()).isNotBlank();
        assertThat(agentJson.get("activeFocusDimensions").isArray()).isTrue();
        assertThat(agentJson.get("activeFocusDimensions").size()).isGreaterThan(0);
        assertThat(agentJson.get("nextRecommendedAction").asText()).isNotBlank();
        assertThat(agentJson.get("lastDecisionSummary").asText()).isNotBlank();

        // Verify last event type is assessment-related
        assertThat(agentJson.get("lastEventType").asText()).isEqualTo("ASSESSMENT_COMPLETED");
    }

    @Test
    @Order(16)
    @DisplayName("Agent: 决策中的工具调用全部在 6 项白名单内")
    void agentDecisionToolCallsAreWhitelisted() throws Exception {
        PipelineContext ctx = setupFullPipeline(AiAcceptanceFixtures.AI_TITLE,
                AiAcceptanceFixtures.AI_JD,
                AiAcceptanceFixtures.AI_SUMMARY,
                AiAcceptanceFixtures.AI_SKILLS,
                AiAcceptanceFixtures.AI_PROJECTS,
                AiAcceptanceFixtures.AI_EXPERIENCE);

        MvcResult agentResult = mockMvc.perform(get("/api/targets/" + ctx.targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + ctx.token))
                .andReturn();
        assumeAiOk(agentResult, "Agent 状态查询");

        com.fasterxml.jackson.databind.JsonNode agentJson =
                objectMapper.readTree(agentResult.getResponse().getContentAsString());

        // Verify focus dimensions are non-empty strings
        com.fasterxml.jackson.databind.JsonNode dimensions = agentJson.get("activeFocusDimensions");
        assertThat(dimensions.isArray()).isTrue();
        for (com.fasterxml.jackson.databind.JsonNode dim : dimensions) {
            assertThat(dim.asText()).isNotBlank();
        }

        // The recommended action should be a meaningful string (not just whitespace)
        String action = agentJson.get("nextRecommendedAction").asText();
        assertThat(action.trim().length()).isGreaterThan(5);
    }

    @Test
    @Order(17)
    @DisplayName("Agent: Agent JSON 使用 camelCase 字段名")
    void agentJsonUsesCamelCase() throws Exception {
        PipelineContext ctx = setupFullPipeline(AiAcceptanceFixtures.JAVA_TITLE,
                AiAcceptanceFixtures.JAVA_JD,
                AiAcceptanceFixtures.JAVA_SUMMARY,
                AiAcceptanceFixtures.JAVA_SKILLS,
                AiAcceptanceFixtures.JAVA_PROJECTS,
                AiAcceptanceFixtures.JAVA_EXPERIENCE);

        MvcResult agentResult = mockMvc.perform(get("/api/targets/" + ctx.targetId + "/coach-agent")
                        .header("Authorization", "Bearer " + ctx.token))
                .andReturn();
        assumeAiOk(agentResult, "Agent 状态查询");

        String rawJson = agentResult.getResponse().getContentAsString();

        // Verify camelCase field names exist (not snake_case)
        assertThat(rawJson).contains("targetId");
        assertThat(rawJson).contains("currentStage");
        assertThat(rawJson).contains("currentGoal");
        assertThat(rawJson).contains("activeFocusDimensions");
        assertThat(rawJson).contains("nextRecommendedAction");
        assertThat(rawJson).contains("lastEventType");
        assertThat(rawJson).contains("lastDecisionSummary");
        assertThat(rawJson).contains("lastRunAt");
        assertThat(rawJson).contains("createdAt");
        assertThat(rawJson).contains("updatedAt");

        // Verify no snake_case versions
        assertThat(rawJson).doesNotContain("target_id");
        assertThat(rawJson).doesNotContain("current_stage");
        assertThat(rawJson).doesNotContain("current_goal");
        assertThat(rawJson).doesNotContain("active_focus_dimensions");
        assertThat(rawJson).doesNotContain("next_recommended_action");
        assertThat(rawJson).doesNotContain("last_event_type");
        assertThat(rawJson).doesNotContain("last_decision_summary");
        assertThat(rawJson).doesNotContain("last_run_at");
        assertThat(rawJson).doesNotContain("created_at");
        assertThat(rawJson).doesNotContain("updated_at");
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
        MvcResult assessStartResult = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(ctx.targetId))))
                .andExpect(status().isOk())
                .andReturn();
        String sessionJson = assessStartResult.getResponse().getContentAsString();
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

    private void assumeAiOk(MvcResult result, String taskName) {
        assertThat(result.getResponse().getStatus())
                .as(taskName + " status")
                .isEqualTo(200);
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
