package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AssessmentDimensionName;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.api.DimensionScore;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.MockInterviewReportDto;
import com.interviewcoach.common.api.SkillMapItem;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import java.util.List;

public class LocalPlatformAiClient implements PlatformAiClient {

    private final ObjectMapper objectMapper;

    public LocalPlatformAiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateJson(AiPrompt prompt) {
        try {
            return switch (prompt.task()) {
                case AiPrompt.TASK_JOB_BRIEF -> objectMapper.writeValueAsString(buildJobBrief(prompt));
                case AiPrompt.TASK_ASSESSMENT_QUESTIONS -> objectMapper.writeValueAsString(buildAssessmentQuestions(prompt));
                case AiPrompt.TASK_ASSESSMENT_RESULT -> objectMapper.writeValueAsString(buildAssessmentResult(prompt));
                case AiPrompt.TASK_TRAINING_PLAN -> objectMapper.writeValueAsString(buildTrainingPlan(prompt));
                case AiPrompt.TASK_TRAINING_FEEDBACK -> objectMapper.writeValueAsString(buildTrainingFeedback(prompt));
                case AiPrompt.TASK_MOCK_INTERVIEW_QUESTION -> objectMapper.writeValueAsString(buildMockInterviewQuestion(prompt));
                case AiPrompt.TASK_MOCK_INTERVIEW_REPORT -> objectMapper.writeValueAsString(buildMockInterviewReport(prompt));
                case AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT -> objectMapper.writeValueAsString(buildCandidateProfileDraft(prompt));
                default -> throw new IllegalStateException("Unknown task: " + prompt.task());
            };
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to render local platform AI JSON", ex);
        }
    }

    private JobBriefDto buildJobBrief(AiPrompt prompt) {
        return new JobBriefDto(
                prompt.targetId(),
                "基于目标岗位 JD 和已确认候选人摘要生成的岗位画像。重点关注岗位职责、核心技能和面试风险点。",
                List.of(
                        new SkillMapItem("Java", "required", "unknown", "需要结合候选人真实项目确认技术深度"),
                        new SkillMapItem("Spring Boot", "required", "unknown", "需要准备服务设计、事务和异常处理案例"),
                        new SkillMapItem("SQL", "important", "unknown", "需要补充查询优化和数据一致性表达")
                ),
                List.of("Java", "Spring Boot", "SQL"),
                List.of("Redis", "系统设计", "业务问题拆解"),
                List.of("围绕 JD 中的业务场景准备技术方案表达", "突出真实项目中的职责边界和交付结果"),
                List.of("项目架构", "接口设计", "数据一致性", "故障排查"),
                List.of("已确认经历可作为岗位匹配分析基础"),
                List.of("候选人项目细节仍需在后续测评和模拟面试中确认"),
                0.6
        );
    }

    private AiStructuredOutputService.AssessmentQuestionsResult buildAssessmentQuestions(AiPrompt prompt) {
        return new AiStructuredOutputService.AssessmentQuestionsResult(List.of(
                new AssessmentQuestionDto(
                        "请简述你在项目中使用 Spring Boot 处理高并发请求的经验，包括具体的优化手段。",
                        AssessmentDimensionName.TECHNICAL_DEPTH, "basic",
                        "考察候选人对 Spring Boot 高并发处理的基本理解和实践经验。",
                        List.of("能说明线程池配置", "能描述请求处理优化手段")),
                new AssessmentQuestionDto(
                        "在数据库设计中，你如何处理多表关联查询的性能问题？请举例说明索引策略的选择。",
                        AssessmentDimensionName.SYSTEM_THINKING, "basic",
                        "考察候选人对数据库查询优化的基本掌握。",
                        List.of("能说明索引类型选择", "能结合具体场景分析查询计划")),
                new AssessmentQuestionDto(
                        "请描述一次你在生产环境中排查线上故障的经历，包括定位问题的过程和最终解决方案。",
                        AssessmentDimensionName.FAILURE_HANDLING, "medium",
                        "考察候选人的实际问题排查经验和系统性思维。",
                        List.of("能描述排查过程", "能说明定位工具和手段", "能给出最终解决方案")),
                new AssessmentQuestionDto(
                        "Redis 在你的项目中扮演了哪些角色？请分别说明缓存策略和数据一致性保证方式。",
                        AssessmentDimensionName.TRADEOFF_AWARENESS, "medium",
                        "考察候选人对中间件选型和权衡的理解。",
                        List.of("能说明 Redis 使用场景", "能解释缓存策略", "能讨论一致性方案")),
                new AssessmentQuestionDto(
                        "如果你要设计一个支持百万级用户的 API 系统，你会如何进行架构分层和容量规划？",
                        AssessmentDimensionName.BUSINESS_CONTEXT, "deep",
                        "考察候选人的系统架构设计能力和容量评估能力。",
                        List.of("能给出合理的架构分层", "能估算容量需求", "能说明扩展策略"))
        ));
    }

    private AssessmentResultDto buildAssessmentResult(AiPrompt prompt) {
        return new AssessmentResultDto(
                prompt.targetId(),
                72,
                List.of(
                        new DimensionScore("Java 基础", 75, "对核心概念理解扎实，能结合项目说明使用场景"),
                        new DimensionScore("系统设计", 70, "具备基本架构思维，但在容量规划方面需要加强"),
                        new DimensionScore("问题排查", 80, "有实际排查经验，方法论清晰"),
                        new DimensionScore("数据库", 65, "基本概念正确，需要深入学习索引优化和查询计划分析"),
                        new DimensionScore("中间件", 70, "Redis 使用经验良好，但分布式场景下的一致性理解需要加强")
                ),
                List.of("具备实际项目经验，问题排查思路清晰", "Java 和 Spring Boot 基础扎实"),
                List.of("系统设计能力需要加强，特别是容量规划和扩展性方面", "数据库深度优化经验不足"),
                List.of("重点复习系统设计中的容量评估方法", "练习数据库查询优化案例", "准备分布式一致性相关面试题")
        );
    }

    private AiStructuredOutputService.TrainingPlanResult buildTrainingPlan(AiPrompt prompt) {
        return new AiStructuredOutputService.TrainingPlanResult(List.of(
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "系统设计容量规划练习",
                        "针对系统设计短板，练习如何估算 QPS、存储和带宽需求，设计一个支持百万用户的 API 系统。"
                ),
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "数据库索引优化案例分析",
                        "针对数据库深度不足，分析一个慢查询案例，说明索引选择、覆盖索引和查询计划优化。"
                ),
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "分布式一致性场景回答",
                        "针对分布式理解薄弱，准备 CAP 理论、最终一致性、分布式锁等常见面试问题的回答。"
                )
        ));
    }

    private TrainingFeedbackDto buildTrainingFeedback(AiPrompt prompt) {
        return new TrainingFeedbackDto(
                prompt.targetId(),
                68,
                "回答覆盖了基本概念，但在深度和结构化表达上需要加强。建议按照'问题→方案→结果'的结构重新组织。",
                List.of("缺少具体的量化数据", "未说明技术选型的对比和权衡"),
                "在项目中，我负责设计订单系统的容量规划。首先估算日均订单量 10 万，峰值 QPS 约为 500。采用 3 台应用服务器 + Redis 缓存 + PostgreSQL 读写分离的架构，将 P99 延迟控制在 200ms 以内。",
                "如果峰值 QPS 突然增长到 2000，你的架构会如何调整？",
                List.of("准备容量估算的完整公式和步骤", "练习用数字说话，避免泛泛而谈", "复习水平扩展和垂直扩展的取舍")
        );
    }

    private AiStructuredOutputService.MockInterviewQuestionResult buildMockInterviewQuestion(AiPrompt prompt) {
        return new AiStructuredOutputService.MockInterviewQuestionResult(
                "请介绍一下你在项目中使用 Spring Boot 的经验，特别是如何处理高并发场景？"
        );
    }

    private MockInterviewReportDto buildMockInterviewReport(AiPrompt prompt) {
        return new MockInterviewReportDto(
                prompt.targetId(),
                70,
                List.of(
                        new DimensionScore("技术深度", 72, "能结合项目说明核心技术的使用，但缺少底层原理阐述"),
                        new DimensionScore("表达结构", 68, "回答有逻辑但不够简洁，建议用 STAR 法则组织"),
                        new DimensionScore("问题分析", 75, "能准确定位问题，排查思路清晰"),
                        new DimensionScore("系统设计", 65, "架构设计基本合理，但在容量评估方面需要加强")
                ),
                "整体表现中等偏上，技术基础扎实，但在系统设计和表达结构方面还有提升空间。",
                List.of("项目经验丰富，有实际排查线上问题的能力", "Java 和 Spring Boot 基础扎实"),
                List.of("系统设计回答缺少容量估算的具体数字", "部分回答过于冗长，需要练习精简表达"),
                List.of("在项目中，我负责设计订单系统架构。采用微服务拆分，订单服务独立部署，通过消息队列异步处理库存扣减，P99 延迟控制在 200ms 以内。"),
                List.of("系统设计容量规划", "STAR 法则面试表达", "分布式一致性原理复习")
        );
    }

    private CandidateProfileDraftDto buildCandidateProfileDraft(AiPrompt prompt) {
        return new CandidateProfileDraftDto(
                "候选人具备后端开发经验，熟悉 Java 技术栈和常见中间件，有实际项目交付经验。建议在后续测评中重点确认系统设计和问题排查能力。",
                List.of("Java", "Spring Boot", "SQL", "Redis"),
                List.of("后端服务开发", "API 设计与实现"),
                List.of("3 年后端开发经验"),
                0
        );
    }
}
