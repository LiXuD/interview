package com.interviewcoach.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.common.api.AssessmentAnswerRequest;
import com.interviewcoach.common.api.AssessmentStartRequest;
import com.interviewcoach.common.api.CandidateProfileConfirmRequest;
import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.LoginRequest;
import com.interviewcoach.common.api.TrainingPlanGenerateRequest;
import com.interviewcoach.common.api.TrainingTaskAnswerRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainingAdaptiveActionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlatformAiClient platformAiClient;

    @Test
    void adaptiveTrainingPreservesSwitchActionForNonTerminalRound() throws Exception {
        String token = loginAndGetToken("training_switch_user");
        String targetId = createTarget(token);
        confirmProfile(token, targetId);
        mockAi(targetId);
        completeAssessment(token, targetId);

        String taskId = generatePlanAndGetFirstTaskId(token, targetId);
        String sessionResponse = mockMvc.perform(post("/api/training-tasks/" + taskId + "/adaptive-sessions/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(sessionResponse).get("id").asText();

        mockMvc.perform(post("/api/training-sessions/" + sessionId + "/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingTaskAnswerRequest("第一轮回答"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.lastAction").value("switch"))
                .andExpect(jsonPath("$.currentQuestion").value("请换一个角度说明容量规划中的降级策略。"))
                .andExpect(jsonPath("$.rounds[0].action").value("switch"));
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
                                "Backend Engineer",
                                "Sample JD for adaptive training switch action"))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void confirmProfile(String token, String targetId) throws Exception {
        var request = new CandidateProfileConfirmRequest(
                targetId,
                "Test summary",
                List.of("Java"),
                List.of("Project A"),
                List.of("Company X"));
        mockMvc.perform(post("/api/profiles/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
                            .content(objectMapper.writeValueAsString(new AssessmentAnswerRequest("Answer " + i))))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/assessments/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String generatePlanAndGetFirstTaskId(String token, String targetId) throws Exception {
        String response = mockMvc.perform(post("/api/training-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrainingPlanGenerateRequest(targetId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("tasks").get(0).get("id").asText();
    }

    private void mockAi(String targetId) {
        Mockito.when(platformAiClient.generateJson(any())).thenAnswer(invocation -> {
            AiPrompt prompt = invocation.getArgument(0, AiPrompt.class);
            return switch (prompt.task()) {
                case AiPrompt.TASK_ASSESSMENT_QUESTIONS -> assessmentQuestions();
                case AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE -> questionScore();
                case AiPrompt.TASK_ASSESSMENT_RESULT -> assessmentResult(prompt.targetId());
                case AiPrompt.TASK_TRAINING_PLAN -> trainingPlan();
                case AiPrompt.TASK_ADAPTIVE_TRAINING_TURN -> adaptiveTrainingTurn(prompt);
                case AiPrompt.TASK_COACHING_MEMORY -> coachingMemory(targetId);
                default -> "{}";
            };
        });
    }

    private String assessmentQuestions() {
        return """
                {
                  "questions": [
                    {"question": "Q1", "dimension": "technicalDepth", "difficulty": "basic", "intent": "I1", "rubric": ["R1"]},
                    {"question": "Q2", "dimension": "projectSpecificity", "difficulty": "basic", "intent": "I2", "rubric": ["R2"]},
                    {"question": "Q3", "dimension": "systemThinking", "difficulty": "medium", "intent": "I3", "rubric": ["R3"]},
                    {"question": "Q4", "dimension": "failureHandling", "difficulty": "medium", "intent": "I4", "rubric": ["R4"]},
                    {"question": "Q5", "dimension": "tradeoffAwareness", "difficulty": "deep", "intent": "I5", "rubric": ["R5"]}
                  ]
                }
                """;
    }

    private String questionScore() {
        return """
                {
                  "questionIndex": 0,
                  "score": 68,
                  "dimension": "technicalDepth",
                  "feedback": "反馈",
                  "problems": ["问题"],
                  "improvedExample": "示范",
                  "answerStructure": {
                    "background": "present: 背景",
                    "task": "partial: 任务",
                    "action": "present: 行动",
                    "result": "missing: 结果",
                    "tradeoff": "missing: 权衡",
                    "review": "missing: 复盘"
                  },
                  "followUpRisks": ["追问"],
                  "contentHighlights": ["亮点"],
                  "contentGaps": ["缺口"]
                }
                """;
    }

    private String assessmentResult(String assessmentId) {
        return """
                {
                  "assessmentId": "%s",
                  "totalScore": 68,
                  "dimensions": [{"name": "technicalDepth", "score": 68, "reason": "reason"}],
                  "strengths": ["基础扎实"],
                  "weaknesses": ["容量规划表达不足"],
                  "nextActions": ["练习容量规划"]
                }
                """.formatted(assessmentId);
    }

    private String trainingPlan() {
        return """
                {
                  "tasks": [
                    {"title": "容量规划训练", "description": "围绕容量规划进行追问", "dayIndex": 0},
                    {"title": "故障处理训练", "description": "围绕故障处理进行追问", "dayIndex": 0},
                    {"title": "数据库优化训练", "description": "围绕数据库优化进行追问", "dayIndex": 0},
                    {"title": "分布式系统训练", "description": "围绕分布式系统进行追问", "dayIndex": 1},
                    {"title": "微服务架构训练", "description": "围绕微服务架构进行追问", "dayIndex": 1},
                    {"title": "性能调优训练", "description": "围绕性能调优进行追问", "dayIndex": 1},
                    {"title": "系统设计训练", "description": "围绕系统设计进行追问", "dayIndex": 2},
                    {"title": "项目复盘训练", "description": "围绕项目复盘进行追问", "dayIndex": 2},
                    {"title": "综合面试训练", "description": "围绕综合面试进行追问", "dayIndex": 2}
                  ]
                }
                """;
    }

    private String adaptiveTrainingTurn(AiPrompt prompt) {
        if (prompt.userPrompt().contains("启动自适应训练")) {
            return """
                    {
                      "action": "continue",
                      "score": 60,
                      "feedback": "开始训练",
                      "problems": ["需要补充细节"],
                      "nextQuestion": "请说明容量规划的核心指标。",
                      "summary": "",
                      "recommendedReviewPoints": ["容量规划"]
                    }
                    """;
        }
        return """
                {
                  "action": "switch",
                  "score": 70,
                  "feedback": "当前回答需要换角度继续追问。",
                  "problems": ["降级策略不足"],
                  "nextQuestion": "请换一个角度说明容量规划中的降级策略。",
                  "summary": "",
                  "recommendedReviewPoints": ["降级策略"]
                }
                """;
    }

    private String coachingMemory(String targetId) {
        return """
                {
                  "observedStrengths": [],
                  "observedWeaknesses": [{"content": "容量规划不足", "source": "observed", "confidence": "medium"}],
                  "recurringProblems": [],
                  "verifiedExperience": [],
                  "unverifiedClaims": [],
                  "recommendedNextFocus": [{"content": "容量规划", "source": "observed", "confidence": "medium"}],
                  "avoidRepeating": []
                }
                """;
    }
}
