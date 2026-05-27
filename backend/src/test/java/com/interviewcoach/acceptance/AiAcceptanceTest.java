package com.interviewcoach.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.common.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Task 19: 真实 AI 验收样例集 — 结构性验收测试。
 *
 * 使用典型岗位样例数据跑通完整教练管道，验证：
 * - 管道端到端不崩溃
 * - DTO 解析正确
 * - 每个 AI 任务的结构化输出通过验证
 *
 * 默认使用 stub AI 跑管道结构；内容质量验证由 AiContentQualityTest 负责。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlatformAiClient platformAiClient;

    // ==================== 场景 1: Java 后端/支付系统 ====================

    @Test
    @DisplayName("Java 支付后端: 完整教练管道从创建岗位到模拟面试报告")
    void javaBackendFullPipeline() throws Exception {
        String token = loginAndGetToken("accept_java_user");
        String targetId = createTarget(token,
                AiAcceptanceFixtures.JAVA_TITLE,
                AiAcceptanceFixtures.JAVA_JD);
        confirmProfile(token, targetId,
                AiAcceptanceFixtures.JAVA_SUMMARY,
                AiAcceptanceFixtures.JAVA_SKILLS,
                AiAcceptanceFixtures.JAVA_PROJECTS,
                AiAcceptanceFixtures.JAVA_EXPERIENCE);
        mockAllAiTasks(targetId);

        // 1. JobBrief
        String jobBriefResponse = mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(targetId))
                .andExpect(jsonPath("$.roleSummary").isString())
                .andExpect(jsonPath("$.skillMap").isArray())
                .andExpect(jsonPath("$.mustHaveSkills").isArray())
                .andExpect(jsonPath("$.confidence").isNumber())
                .andReturn().getResponse().getContentAsString();

        // 2. Assessment: start -> 5 answers -> finish
        String sessionId = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(5))
                .andExpect(jsonPath("$.currentQuestion").isString())
                .andReturn().getResponse().getContentAsString();
        sessionId = objectMapper.readTree(sessionId).get("id").asText();

        answerAllQuestions(token, sessionId,
                AiAcceptanceFixtures.JAVA_ANSWER_ORDER_DESIGN,
                AiAcceptanceFixtures.JAVA_ANSWER_WEAK);

        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentId").value(sessionId))
                .andExpect(jsonPath("$.totalScore").isNumber())
                .andExpect(jsonPath("$.dimensions").isArray())
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.weaknesses").isArray())
                .andExpect(jsonPath("$.nextActions").isArray());

        // 3. TrainingPlan
        MvcResult planResult = mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingPlanGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks.length()").value(3))
                .andReturn();

        String planJson = planResult.getResponse().getContentAsString();
        String taskId = objectMapper.readTree(planJson).get("tasks").get(0).get("id").asText();

        // 4. TrainingFeedback
        mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TrainingTaskAnswerRequest(AiAcceptanceFixtures.JAVA_ANSWER_ORDER_DESIGN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.feedback").isString())
                .andExpect(jsonPath("$.rewrittenAnswer").isString())
                .andExpect(jsonPath("$.followUpQuestion").isString());

        // 5. MockInterview: start -> answer -> finish -> report
        String interviewId = mockMvc.perform(post("/api/mock-interviews/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewStartRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.currentQuestion").isString())
                .andReturn().getResponse().getContentAsString();
        interviewId = objectMapper.readTree(interviewId).get("id").asText();

        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MockInterviewAnswerRequest(AiAcceptanceFixtures.JAVA_ANSWER_ORDER_DESIGN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestion").isString());

        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mockInterviewId").value(interviewId))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.dimensionScores").isArray())
                .andExpect(jsonPath("$.summary").isString());
    }

    // ==================== 场景 2: AI 应用工程师/RAG Agent ====================

    @Test
    @DisplayName("AI RAG 工程师: 完整教练管道")
    void aiEngineerFullPipeline() throws Exception {
        String token = loginAndGetToken("accept_ai_user");
        String targetId = createTarget(token,
                AiAcceptanceFixtures.AI_TITLE,
                AiAcceptanceFixtures.AI_JD);
        confirmProfile(token, targetId,
                AiAcceptanceFixtures.AI_SUMMARY,
                AiAcceptanceFixtures.AI_SKILLS,
                AiAcceptanceFixtures.AI_PROJECTS,
                AiAcceptanceFixtures.AI_EXPERIENCE);
        mockAllAiTasks(targetId);

        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(targetId));

        String sessionId = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(5))
                .andReturn().getResponse().getContentAsString();
        sessionId = objectMapper.readTree(sessionId).get("id").asText();

        answerAllQuestions(token, sessionId,
                AiAcceptanceFixtures.AI_ANSWER_RAG_DESIGN,
                AiAcceptanceFixtures.AI_ANSWER_WEAK);

        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").isNumber());

        MvcResult planResult = mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingPlanGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").value(3))
                .andReturn();

        String taskId = objectMapper.readTree(planResult.getResponse().getContentAsString())
                .get("tasks").get(0).get("id").asText();

        mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TrainingTaskAnswerRequest(AiAcceptanceFixtures.AI_ANSWER_RAG_DESIGN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber());
    }

    // ==================== 场景 3: 数据平台/调度数仓 ====================

    @Test
    @DisplayName("数据平台工程师: 完整教练管道")
    void dataPlatformFullPipeline() throws Exception {
        String token = loginAndGetToken("accept_data_user");
        String targetId = createTarget(token,
                AiAcceptanceFixtures.DATA_TITLE,
                AiAcceptanceFixtures.DATA_JD);
        confirmProfile(token, targetId,
                AiAcceptanceFixtures.DATA_SUMMARY,
                AiAcceptanceFixtures.DATA_SKILLS,
                AiAcceptanceFixtures.DATA_PROJECTS,
                AiAcceptanceFixtures.DATA_EXPERIENCE);
        mockAllAiTasks(targetId);

        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isOk());

        String sessionId = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestions").value(5))
                .andReturn().getResponse().getContentAsString();
        sessionId = objectMapper.readTree(sessionId).get("id").asText();

        answerAllQuestions(token, sessionId,
                AiAcceptanceFixtures.DATA_ANSWER_WAREHOUSE,
                AiAcceptanceFixtures.DATA_ANSWER_WEAK);

        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").isNumber());

        MvcResult planResult = mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingPlanGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").value(3))
                .andReturn();

        String taskId = objectMapper.readTree(planResult.getResponse().getContentAsString())
                .get("tasks").get(0).get("id").asText();

        mockMvc.perform(post("/api/training-tasks/" + taskId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TrainingTaskAnswerRequest(AiAcceptanceFixtures.DATA_ANSWER_WAREHOUSE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber());
    }

    // ==================== 验证 Assessment Report 和 MockInterview Report 的生命周期 ====================

    @Test
    @DisplayName("测评 finish 创建 assessment Report 且可查询")
    void assessmentCreatesReport() throws Exception {
        String token = loginAndGetToken("accept_report_user1");
        String targetId = createTarget(token, "Test Target", "Test JD");
        confirmProfile(token, targetId, "Test summary", List.of("Java"), List.of("Project"), List.of("Exp"));
        mockAllAiTasks(targetId);

        String sessionId = mockMvc.perform(post("/api/assessments/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssessmentStartRequest(targetId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        sessionId = objectMapper.readTree(sessionId).get("id").asText();

        answerAllQuestions(token, sessionId, "Good answer", "Weak answer");

        MvcResult finishResult = mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        // 验证 Report 通过 reports API 可查询
        mockMvc.perform(get("/api/reports?targetId=" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("assessment"));
    }

    @Test
    @DisplayName("模拟面试 finish 创建 mockInterview Report 且可查询")
    void mockInterviewCreatesReport() throws Exception {
        String token = loginAndGetToken("accept_report_user2");
        String targetId = createTarget(token, "Test Target", "Test JD");
        confirmProfile(token, targetId, "Test summary", List.of("Java"), List.of("Project"), List.of("Exp"));
        mockAllAiTasks(targetId);

        String interviewId = mockMvc.perform(post("/api/mock-interviews/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MockInterviewStartRequest(targetId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        interviewId = objectMapper.readTree(interviewId).get("id").asText();

        mockMvc.perform(post("/api/mock-interviews/" + interviewId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 验证 Report 通过 reports API 可查询
        mockMvc.perform(get("/api/reports?targetId=" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("mockInterview"));
    }

    // ==================== AI 解析失败场景 ====================

    @Test
    @DisplayName("JobBrief AI 返回无效 JSON 时返回 AI_PARSE_FAILED")
    void jobBriefAiParseFailureReturnsStructuredError() throws Exception {
        String token = loginAndGetToken("accept_parsefail_user");
        String targetId = createTarget(token, "Parse Fail Test", "Test JD");
        confirmProfile(token, targetId, "Summary", List.of("Java"), List.of(), List.of());

        Mockito.when(platformAiClient.generateJson(any()))
                .thenReturn("not valid json at all")
                .thenReturn("{ still broken");

        mockMvc.perform(post("/api/job-briefs/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobBriefGenerateRequest(targetId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AI_PARSE_FAILED"))
                .andExpect(jsonPath("$.requestId").isString());
    }

    // ==================== Helper Methods ====================

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

    private void answerAllQuestions(String token, String sessionId,
                                   String strongAnswer, String weakAnswer) throws Exception {
        for (int i = 1; i <= 5; i++) {
            String answer = (i <= 2) ? strongAnswer : weakAnswer;
            mockMvc.perform(post("/api/assessments/" + sessionId + "/answers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest(answer))))
                    .andExpect(status().isOk());
        }
    }

    // ==================== AI Mock: 单一分发器模式 ====================
    // 使用单个 thenAnswer 分发器避免 argThat lambda 的 null 匹配问题

    private void mockAllAiTasks(String targetId) {
        Mockito.reset(platformAiClient);
        Mockito.when(platformAiClient.generateJson(any())).thenAnswer(invocation -> {
            com.interviewcoach.ai.service.AiPrompt prompt =
                    invocation.getArgument(0, AiPrompt.class);
            String task = prompt != null ? prompt.task() : "";
            return switch (task) {
                case "jobBrief" -> mockJobBriefResponse(targetId);
                case "assessmentQuestions" -> mockAssessmentQuestionsResponse();
                case "assessmentResult" -> mockAssessmentResultResponse(
                        prompt.targetId());
                case "trainingPlan" -> mockTrainingPlanResponse();
                case "trainingFeedback" -> mockTrainingFeedbackResponse(
                        prompt.targetId());
                case "mockInterviewQuestion" -> mockInterviewQuestionResponse();
                case "mockInterviewReport" -> mockInterviewReportResponse(
                        prompt.targetId());
                default -> "{}";
            };
        });
    }

    private String mockJobBriefResponse(String targetId) {
        return """
                {
                  "targetId": "%s",
                  "roleSummary": "负责核心业务模块的设计与开发，保障系统稳定性和可扩展性。",
                  "skillMap": [
                    {"name": "核心技能1", "importance": "required", "userLevel": "solid", "gap": "需补强高级特性"},
                    {"name": "核心技能2", "importance": "important", "userLevel": "basic", "gap": "需要实践经验"}
                  ],
                  "mustHaveSkills": ["核心技能1", "核心技能2"],
                  "niceToHaveSkills": ["加分技能"],
                  "businessContext": ["业务背景1", "业务背景2"],
                  "interviewTopics": ["面试主题1", "面试主题2", "面试主题3"],
                  "candidateMatch": ["匹配点1"],
                  "riskAreas": ["风险点1"],
                  "confidence": 0.75
                }
                """.formatted(targetId);
    }

    private String mockAssessmentQuestionsResponse() {
        return """
                {
                  "questions": [
                    "请解释核心概念A的基本原理。",
                    "请解释核心概念B在项目中的应用场景。",
                    "在你的项目中，如何处理场景C？请结合具体案例说明。",
                    "假设遇到问题D，你会如何设计解决方案？",
                    "请深入分析技术E的优缺点和适用边界。"
                  ]
                }
                """;
    }

    private String mockAssessmentResultResponse(String assessmentId) {
        return """
                {
                  "assessmentId": "%s",
                  "totalScore": 68,
                  "dimensions": [
                    {"name": "technicalDepth", "score": 65, "reason": "基础扎实但高级特性了解不深"},
                    {"name": "projectSpecificity", "score": 72, "reason": "项目描述具体但缺少量化指标"},
                    {"name": "systemThinking", "score": 60, "reason": "系统设计思路有但不够全面"},
                    {"name": "tradeoffAwareness", "score": 70, "reason": "能识别部分权衡点"},
                    {"name": "failureHandling", "score": 55, "reason": "故障处理经验偏少"}
                  ],
                  "strengths": ["基础扎实", "项目经验相关"],
                  "weaknesses": ["高级特性掌握不足", "系统设计经验有限"],
                  "nextActions": ["补强高级特性", "练习系统设计题"]
                }
                """.formatted(assessmentId);
    }

    private String mockTrainingPlanResponse() {
        return """
                {
                  "tasks": [
                    {"title": "训练任务1: 补强短板A", "description": "通过案例练习掌握核心概念"},
                    {"title": "训练任务2: 实践场景B", "description": "设计并实现一个简化方案"},
                    {"title": "训练任务3: 深入理解C", "description": "阅读源码并总结设计模式"}
                  ]
                }
                """;
    }

    private String mockTrainingFeedbackResponse(String taskId) {
        return """
                {
                  "taskId": "%s",
                  "score": 72,
                  "feedback": "回答覆盖了核心要点，但缺少具体实现细节和边界情况分析。",
                  "problems": ["缺少具体参数配置", "未讨论异常处理"],
                  "rewrittenAnswer": "改进后的示范回答，包含完整的实现细节和边界分析。",
                  "followUpQuestion": "如果遇到超时情况，你的方案如何处理？",
                  "recommendedReviewPoints": ["核心概念深入", "异常处理模式"]
                }
                """.formatted(taskId);
    }

    private String mockInterviewQuestionResponse() {
        return "{\"question\": \"请介绍一下你的项目经验。\"}";
    }

    private String mockInterviewReportResponse(String interviewId) {
        return """
                {
                  "mockInterviewId": "%s",
                  "overallScore": 70,
                  "dimensionScores": [
                    {"name": "projectExplanation", "score": 72, "reason": "项目描述清晰但缺少量化"},
                    {"name": "technicalDepth", "score": 65, "reason": "基础概念掌握好"},
                    {"name": "problemSolving", "score": 70, "reason": "思路清晰"},
                    {"name": "communication", "score": 75, "reason": "表达流畅"}
                  ],
                  "summary": "整体表现中等偏上，技术基础扎实但深度有待提升。",
                  "strengths": ["基础扎实", "表达清晰"],
                  "weaknesses": ["技术深度不足", "缺少量化指标"],
                  "improvedAnswers": ["改进示范1"],
                  "nextTrainingTasks": ["深入学习核心原理", "练习系统设计"]
                }
                """.formatted(interviewId);
    }
}
