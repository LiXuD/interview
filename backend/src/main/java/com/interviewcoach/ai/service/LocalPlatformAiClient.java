package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AdaptiveTrainingTurnDto;
import com.interviewcoach.common.api.AnswerStructureDto;
import com.interviewcoach.common.api.AssessmentDimensionName;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentQuestionScoreDto;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.CoachingMemoryItemDto;
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
                case AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE -> objectMapper.writeValueAsString(buildQuestionScore(prompt));
                case AiPrompt.TASK_ASSESSMENT_RESULT -> objectMapper.writeValueAsString(buildAssessmentResult(prompt));
                case AiPrompt.TASK_TRAINING_PLAN -> objectMapper.writeValueAsString(buildTrainingPlan(prompt));
                case AiPrompt.TASK_TRAINING_FEEDBACK -> objectMapper.writeValueAsString(buildTrainingFeedback(prompt));
                case AiPrompt.TASK_ADAPTIVE_TRAINING_TURN -> objectMapper.writeValueAsString(buildAdaptiveTrainingTurn(prompt));
                case AiPrompt.TASK_MOCK_INTERVIEW_QUESTION -> objectMapper.writeValueAsString(buildMockInterviewQuestion(prompt));
                case AiPrompt.TASK_MOCK_INTERVIEW_REPORT -> objectMapper.writeValueAsString(buildMockInterviewReport(prompt));
                case AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT -> objectMapper.writeValueAsString(buildCandidateProfileDraft(prompt));
                case AiPrompt.TASK_COACHING_MEMORY -> objectMapper.writeValueAsString(buildCoachingMemory(prompt));
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

    private AssessmentQuestionScoreDto buildQuestionScore(AiPrompt prompt) {
        int questionIndex = 0;
        try {
            // Extract questionIndex from userPrompt: "题号：N（共 M 题）"
            for (String line : prompt.userPrompt().split("\n")) {
                if (line.startsWith("题号：")) {
                    String numberPart = line.substring("题号：".length()).split("[^0-9]")[0].trim();
                    questionIndex = Integer.parseInt(numberPart) - 1;
                    break;
                }
            }
        } catch (NumberFormatException ignored) {}
        return new AssessmentQuestionScoreDto(
                questionIndex,
                68,
                AssessmentDimensionName.TECHNICAL_DEPTH,
                "回答覆盖了基本概念，但在技术深度和量化表达方面仍需加强。建议按照'问题→方案→指标'的结构重新组织。",
                List.of("缺少具体的技术指标", "未说明性能优化的对比数据"),
                "在项目中，我负责优化订单服务的接口性能。通过引入 Redis 缓存热点数据、优化 SQL 索引和引入异步处理，将 P99 延迟从 800ms 降至 150ms，QPS 从 200 提升至 1200。",
                new AnswerStructureDto(
                        "present: 明确说明了项目背景和技术场景",
                        "partial: 任务描述较笼统",
                        "present: 详细说明了缓存和索引优化方案",
                        "missing: 未给出优化前后的具体数据对比",
                        "missing: 未讨论技术选型的权衡",
                        "missing: 未进行复盘反思"
                ),
                List.of("面试官可能追问具体的性能数据对比", "面试官可能追问 Redis 缓存穿透如何处理"),
                List.of("技术方案选择合理", "排查思路清晰"),
                List.of("缺少量化指标", "未说明技术权衡")
        );
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
                List.of("重点复习系统设计中的容量评估方法", "练习数据库查询优化案例", "准备分布式一致性相关面试题"),
                List.of()
        );
    }

    private AiStructuredOutputService.TrainingPlanResult buildTrainingPlan(AiPrompt prompt) {
        return new AiStructuredOutputService.TrainingPlanResult(List.of(
                // Day 1: 基础巩固
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "系统设计容量规划练习",
                        "针对系统设计短板，练习如何估算 QPS、存储和带宽需求，设计一个支持百万用户的 API 系统。",
                        0
                ),
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "数据库索引优化案例分析",
                        "针对数据库深度不足，分析一个慢查询案例，说明索引选择、覆盖索引和查询计划优化。",
                        0
                ),
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "分布式一致性场景回答",
                        "针对分布式理解薄弱，准备 CAP 理论、最终一致性、分布式锁等常见面试问题的回答。",
                        0
                ),
                // Day 2: 进阶练习
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "微服务架构设计练习",
                        "基于第一天的容量规划，设计一个完整的微服务架构，包括服务拆分、通信方式和容错策略。",
                        1
                ),
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "数据库分库分表方案设计",
                        "基于第一天的索引优化知识，设计一个支持千万级数据的分库分表方案。",
                        1
                ),
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "分布式事务实战场景",
                        "基于第一天的分布式一致性知识，准备 TCC、Saga 等分布式事务方案的面试回答。",
                        1
                ),
                // Day 3: 综合实战
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "完整系统设计面试模拟",
                        "综合前两天的练习，完成一个端到端的系统设计面试题，包括需求分析、架构设计和性能优化。",
                        2
                ),
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "项目深挖回答准备",
                        "结合前两天的技术练习，准备如何在面试中深度讲解自己的项目经历，突出技术亮点和量化成果。",
                        2
                ),
                new AiStructuredOutputService.TrainingPlanTaskItem(
                        "技术权衡与决策回答",
                        "准备如何在面试中展示技术决策能力，包括选型对比、风险评估和权衡取舍。",
                        2
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

    private AdaptiveTrainingTurnDto buildAdaptiveTrainingTurn(AiPrompt prompt) {
        if (prompt.userPrompt().contains("已完成回答轮数：1")) {
            return new AdaptiveTrainingTurnDto(
                    "pass",
                    82,
                    "第二轮回答已经能补充关键技术细节，当前短板基本达标。",
                    List.of("还可以继续补充量化指标"),
                    "",
                    "本次自适应训练完成了 2 轮追问，候选人已经能围绕短板给出更具体的技术表达。",
                    List.of("复盘指标表达", "准备权衡说明")
            );
        }
        return new AdaptiveTrainingTurnDto(
                "continue",
                68,
                "回答覆盖了基本方向，但还需要追问具体指标和权衡。",
                List.of("缺少量化指标", "权衡说明不足"),
                "请继续补充这个方案上线后的关键指标、风险权衡和复盘结论。",
                "",
                List.of("容量评估", "技术权衡")
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
                List.of("系统设计容量估算依据", "线上问题排查中的取舍与复盘"),
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

    private CoachingMemoryDto buildCoachingMemory(AiPrompt prompt) {
        return new CoachingMemoryDto(
                null,
                prompt.targetId(),
                "assessment",
                null,
                List.of(
                        memoryItem("Java 基础扎实，能结合项目说明使用场景", "observed", "high"),
                        memoryItem("问题排查思路清晰", "observed", "high")),
                List.of(
                        memoryItem("系统设计能力需要加强，容量规划方面经验不足", "observed", "medium"),
                        memoryItem("数据库深度优化经验不足", "observed", "medium")),
                List.of(memoryItem("回答偏概念，缺少量化数据", "observed", "high")),
                List.of(memoryItem("有实际项目经验", "confirmed", "medium")),
                List.of(memoryItem("提到高并发优化，但未给出具体指标", "inferred", "low")),
                List.of(
                        memoryItem("容量规划", "observed", "high"),
                        memoryItem("数据库索引优化", "observed", "medium")),
                List.of(memoryItem("不要继续问泛泛的 Redis 基础概念", "observed", "medium")),
                null
        );
    }

    private CoachingMemoryItemDto memoryItem(String content, String source, String confidence) {
        return new CoachingMemoryItemDto(content, source, confidence);
    }
}
