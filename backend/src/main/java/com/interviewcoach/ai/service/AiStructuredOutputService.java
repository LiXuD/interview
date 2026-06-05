package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AgentDecisionDto;
import com.interviewcoach.common.api.AgentToolCallDto;
import com.interviewcoach.common.api.AnswerStructureDto;
import com.interviewcoach.common.api.AdaptiveTrainingTurnDto;
import com.interviewcoach.common.api.AssessmentDimensionName;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentQuestionScoreDto;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.api.DimensionScore;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.MockInterviewReportDto;
import com.interviewcoach.common.api.SkillMapItem;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.CoachingMemoryItemDto;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.error.AiParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * AI 结构化输出服务。统一管理所有 AI 业务任务的调用、JSON 解析、业务校验和重试修复。
 * <p>每个 generate* 方法对应一个教练业务场景（岗位画像、测评、训练、模拟面试等），
 * 内部优先尝试 Spring AI 结构化输出，失败后回退到 JSON 字符串 + 手动解析。</p>
 */
@Service
public class AiStructuredOutputService {

    private static final Set<String> VALID_IMPORTANCE = Set.of("required", "important", "bonus");
    private static final Set<String> VALID_USER_LEVEL = Set.of("unknown", "weak", "basic", "solid", "strong");
    private static final Set<String> VALID_MEMORY_SOURCES = Set.of(
            "confirmed", "observed", "corrected", "inferred", "rejected");
    private static final Set<String> VALID_MEMORY_CONFIDENCE = Set.of("high", "medium", "low");
    private static final Set<String> VALID_STRUCTURE_STATUS = Set.of("present", "partial", "missing");
    private static final Set<String> VALID_ADAPTIVE_TRAINING_ACTIONS = Set.of("continue", "pass", "switch", "stop");

    private final AiModelGateway aiModelGateway;
    private final ObjectMapper objectMapper;
    private final AiMetrics aiMetrics;

    @Autowired
    public AiStructuredOutputService(AiModelGateway aiModelGateway,
                                     ObjectMapper objectMapper,
                                     AiMetrics aiMetrics) {
        this.aiModelGateway = aiModelGateway;
        this.objectMapper = objectMapper;
        this.aiMetrics = aiMetrics;
    }

    public AiStructuredOutputService(PlatformAiClient platformAiClient,
                                     OpenAiCompatibleClient openAiClient,
                                     AiProviderService providerService,
                                     ApiKeyEncryption encryption,
                                     ObjectMapper objectMapper,
                                     PlatformAiProperties platformProperties,
                                     SpringAiFoundationProperties springAiProperties,
                                     SpringAiUserProviderClient springAiUserProviderClient,
                                     AiMetrics aiMetrics) {
        this(new DefaultAiModelGateway(
                        platformAiClient,
                        openAiClient,
                        providerService,
                        encryption,
                        platformProperties,
                        springAiProperties,
                        springAiUserProviderClient,
                        aiMetrics),
                objectMapper, aiMetrics);
    }

    public AiStructuredOutputService(PlatformAiClient platformAiClient,
                                     OpenAiCompatibleClient openAiClient,
                                     AiProviderService providerService,
                                     ApiKeyEncryption encryption,
                                     ObjectMapper objectMapper,
                                     PlatformAiProperties platformProperties,
                                     SpringAiFoundationProperties springAiProperties,
                                     SpringAiUserProviderClient springAiUserProviderClient) {
        this(platformAiClient, openAiClient, providerService, encryption, objectMapper,
                platformProperties, springAiProperties, springAiUserProviderClient, new NoOpAiMetrics());
    }

    /** 测试用构造函数，使用默认测试配置 */
    public AiStructuredOutputService(PlatformAiClient platformAiClient,
                                     OpenAiCompatibleClient openAiClient,
                                     AiProviderService providerService,
                                     ApiKeyEncryption encryption,
                                     ObjectMapper objectMapper) {
        this(platformAiClient, openAiClient, providerService, encryption, objectMapper,
                testPlatformProperties(), new SpringAiFoundationProperties(), null, new NoOpAiMetrics());
    }

    private static PlatformAiProperties testPlatformProperties() {
        PlatformAiProperties properties = new PlatformAiProperties();
        properties.setRequireRealForCoaching(false);
        return properties;
    }

    /**
     * 生成岗位画像（JobBrief）。包含角色摘要、技能地图、面试话题等结构化信息。
     *
     * @param prompt AI 调用请求
     * @return 校验通过的岗位画像 DTO
     */
    public JobBriefDto generateJobBrief(AiPrompt prompt) {
        JobBriefDto structuredResult = generateStructuredFromSpringProvider(prompt, JobBriefDto.class);
        if (structuredResult != null) {
            JobBriefDto validated = validateStructured(prompt, structuredResult, (dto, p) -> validateJobBrief(dto));
            if (validated != null) return validated;
        }
        return generateAndValidate(prompt, JobBriefDto.class, (dto, p) -> validateJobBrief(dto));
    }

    private void validateJobBrief(JobBriefDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("JobBrief is null");
        }
        requireText(dto.roleSummary(), "roleSummary");
        requireList(dto.skillMap(), "skillMap");
        requireList(dto.mustHaveSkills(), "mustHaveSkills");
        requireList(dto.niceToHaveSkills(), "niceToHaveSkills");
        requireList(dto.businessContext(), "businessContext");
        requireList(dto.interviewTopics(), "interviewTopics");
        requireList(dto.candidateMatch(), "candidateMatch");
        requireList(dto.riskAreas(), "riskAreas");
        if (dto.confidence() < 0 || dto.confidence() > 1) {
            throw new IllegalArgumentException("confidence out of range");
        }
        for (SkillMapItem item : dto.skillMap()) {
            requireText(item.name(), "skillMap.name");
            if (!VALID_IMPORTANCE.contains(item.importance())) {
                throw new IllegalArgumentException("invalid skill importance");
            }
            if (!VALID_USER_LEVEL.contains(item.userLevel())) {
                throw new IllegalArgumentException("invalid skill userLevel");
            }
            requireText(item.gap(), "skillMap.gap");
        }
    }

    /** 测评题目列表的封装结构 */
    public record AssessmentQuestionsResult(List<AssessmentQuestionDto> questions) {}

    /**
     * 生成 5 道结构化测评题目，包含维度、难度、意图和评分标准。
     *
     * @param prompt AI 调用请求
     * @return 5 道测评题目 DTO 列表
     */
    public List<AssessmentQuestionDto> generateAssessmentQuestions(AiPrompt prompt) {
        AssessmentQuestionsResult structuredResult = generateStructuredFromSpringProvider(prompt, AssessmentQuestionsResult.class);
        if (structuredResult != null) {
            AssessmentQuestionsResult validated = validateStructured(prompt, structuredResult, (r, p) -> validateQuestions(r.questions()));
            if (validated != null) return validated.questions();
        }
        AssessmentQuestionsResult result = generateAndValidate(prompt, AssessmentQuestionsResult.class, (r, p) -> validateQuestions(r.questions()));
        return result.questions();
    }

    /**
     * 生成单题评分结果，包含分数、反馈、改进示范和回答结构诊断。
     *
     * @param prompt AI 调用请求
     * @return 单题评分结果 DTO
     */
    public AssessmentQuestionScoreDto generateQuestionScore(AiPrompt prompt) {
        AssessmentQuestionScoreDto structuredResult = generateStructuredFromSpringProvider(prompt, AssessmentQuestionScoreDto.class);
        if (structuredResult != null) {
            AssessmentQuestionScoreDto validated = validateStructured(prompt, structuredResult, (dto, p) -> validateQuestionScore(dto));
            if (validated != null) return validated;
        }
        return generateAndValidate(prompt, AssessmentQuestionScoreDto.class, (dto, p) -> validateQuestionScore(dto));
    }

    private void validateQuestionScore(AssessmentQuestionScoreDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AssessmentQuestionScore is null");
        }
        if (dto.score() < 0 || dto.score() > 100) {
            throw new IllegalArgumentException("question score out of range");
        }
        if (dto.questionIndex() < 0 || dto.questionIndex() > 5) {
            throw new IllegalArgumentException("questionIndex out of range");
        }
        requireText(dto.dimension(), "dimension");
        requireText(dto.feedback(), "feedback");
        requireNonEmptyTextList(dto.problems(), "problems");
        requireText(dto.improvedExample(), "improvedExample");
        validateAnswerStructure(dto.answerStructure());
        requireNonEmptyTextList(dto.followUpRisks(), "followUpRisks");
        requireList(dto.contentHighlights(), "contentHighlights");
        requireNonEmptyTextList(dto.contentGaps(), "contentGaps");
    }

    private void validateAnswerStructure(AnswerStructureDto answerStructure) {
        if (answerStructure == null) {
            throw new IllegalArgumentException("answerStructure is required");
        }
        requireStructureStatus(answerStructure.background(), "answerStructure.background");
        requireStructureStatus(answerStructure.task(), "answerStructure.task");
        requireStructureStatus(answerStructure.action(), "answerStructure.action");
        requireStructureStatus(answerStructure.result(), "answerStructure.result");
        requireStructureStatus(answerStructure.tradeoff(), "answerStructure.tradeoff");
        requireStructureStatus(answerStructure.review(), "answerStructure.review");
    }

    private void requireStructureStatus(String value, String field) {
        requireText(value, field);
        int separatorIndex = value.indexOf(':');
        if (separatorIndex < 0) {
            separatorIndex = value.indexOf('：');
        }
        if (separatorIndex <= 0) {
            throw new IllegalArgumentException(field + " must start with status");
        }
        String status = value.substring(0, separatorIndex).trim();
        if (!VALID_STRUCTURE_STATUS.contains(status)) {
            throw new IllegalArgumentException(field + " has invalid status");
        }
    }

    /**
     * 生成测评总结果，包含总分、维度评分、强弱项和下一步行动建议。
     *
     * @param prompt AI 调用请求
     * @return 测评总结果 DTO
     */
    public AssessmentResultDto generateAssessmentResult(AiPrompt prompt) {
        AssessmentResultDto structuredResult = generateStructuredFromSpringProvider(prompt, AssessmentResultDto.class);
        if (structuredResult != null) {
            AssessmentResultDto validated = validateStructured(prompt, structuredResult, (dto, p) -> validateAssessmentResult(dto));
            if (validated != null) return validated;
        }
        return generateAndValidate(prompt, AssessmentResultDto.class, (dto, p) -> validateAssessmentResult(dto));
    }

    private static final Set<String> VALID_DIMENSIONS = Set.copyOf(AssessmentDimensionName.ALL);
    private static final Set<String> VALID_DIFFICULTIES = Set.of("basic", "medium", "deep");

    private void validateQuestions(List<AssessmentQuestionDto> questions) {
        if (questions == null || questions.size() != 5) {
            throw new IllegalArgumentException("Expected exactly 5 questions");
        }
        for (AssessmentQuestionDto q : questions) {
            requireText(q.question(), "question");
            if (!VALID_DIMENSIONS.contains(q.dimension())) {
                throw new IllegalArgumentException("invalid dimension: " + q.dimension());
            }
            if (!VALID_DIFFICULTIES.contains(q.difficulty())) {
                throw new IllegalArgumentException("invalid difficulty: " + q.difficulty());
            }
            requireText(q.intent(), "intent");
            requireList(q.rubric(), "rubric");
            if (q.rubric().isEmpty()) {
                throw new IllegalArgumentException("rubric must not be empty");
            }
            for (String r : q.rubric()) {
                requireText(r, "rubric item");
            }
        }
    }

    private void validateAssessmentResult(AssessmentResultDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AssessmentResult is null");
        }
        if (dto.totalScore() < 0 || dto.totalScore() > 100) {
            throw new IllegalArgumentException("totalScore out of range");
        }
        requireList(dto.dimensions(), "dimensions");
        requireList(dto.strengths(), "strengths");
        requireList(dto.weaknesses(), "weaknesses");
        requireList(dto.nextActions(), "nextActions");
        for (DimensionScore dim : dto.dimensions()) {
            requireText(dim.name(), "dimension.name");
            if (dim.score() < 0 || dim.score() > 100) {
                throw new IllegalArgumentException("dimension score out of range");
            }
            requireText(dim.reason(), "dimension.reason");
        }
    }

    private static final String JSON_REPAIR_SYSTEM_SUFFIX = """

            你之前的回复无法被解析为合法 JSON。请只修复 JSON 格式问题，不要新增、删除或修改任何业务内容。
            直接输出修复后的 JSON，不要包含任何解释或 markdown 格式。""";

    private static final String VALIDATION_REPAIR_SYSTEM_SUFFIX = """

            你之前的 JSON 不满足业务字段约束。请根据原始任务要求修复缺失、无效或不符合约束的业务字段。
            直接输出修复后的完整 JSON，不要包含任何解释或 markdown 格式。""";

    /**
     * 调用 AI 并解析为指定类型，失败时最多重试 1 次（修复 JSON 或校验错误）。
     *
     * @param prompt    AI 调用请求
     * @param type      目标 DTO 类型
     * @param validator 业务校验逻辑
     * @return 校验通过的 DTO
     * @param <T>       目标类型
     * @throws AiParseException 解析或校验两次均失败时
     */
    private <T> T generateAndValidate(AiPrompt prompt, Class<T> type, BiConsumer<T, AiPrompt> validator) {
        try {
            String malformedJson = null;
            String validationError = null;
            // 1. 最多尝试 2 次：首次正常调用，第二次携带修复提示
            for (int attempt = 0; attempt < 2; attempt++) {
                // 2. 第二次尝试时构造修复 Prompt
                AiPrompt actualPrompt = (attempt == 1 && malformedJson != null)
                        ? repairPrompt(prompt, malformedJson, validationError)
                        : prompt;
                // 3. 调用 AI 获取原始 JSON
                String rawJson = generateFromProvider(actualPrompt);
                recordTokenUsageIfPossible(prompt, rawJson);
                try {
                    // 4. 反序列化并执行业务校验
                    T result = objectMapper.readValue(rawJson, type);
                    validator.accept(result, prompt);
                    return result;
                } catch (JsonProcessingException ex) {
                    malformedJson = rawJson;
                    validationError = null;
                    if (attempt == 1) {
                        recordParseFailure(prompt);
                        throw new AiParseException(prompt.task());
                    }
                } catch (IllegalArgumentException ex) {
                    // 5. 业务校验失败，记录错误供下次修复
                    malformedJson = rawJson;
                    validationError = ex.getMessage();
                    if (attempt == 1) {
                        recordValidationFailure(prompt);
                        throw new AiParseException(prompt.task());
                    }
                }
            }
            recordParseFailure(prompt);
            throw new AiParseException(prompt.task());
        } finally {
            DefaultAiModelGateway.clearRequestContext();
        }
    }

    /**
     * 构造修复 Prompt，用于重试时指导 AI 修正 JSON 格式或业务字段。
     *
     * @param original       原始 AI 调用请求
     * @param malformedJson  解析失败的原始 JSON 字符串
     * @param validationError 业务校验错误信息，null 表示 JSON 格式错误
     * @return 包含修复提示的新 AI 调用请求
     */
    private AiPrompt repairPrompt(AiPrompt original, String malformedJson, String validationError) {
        if (validationError == null) {
            // 1. JSON 格式错误：追加格式修复提示
            String userPrompt = "你之前的回复无法被解析为合法 JSON。请只修复 JSON 格式问题。"
                    + "\n\n你之前的回复：\n" + malformedJson
                    + "\n\n请直接输出修复后的完整 JSON，不要包含任何解释或 markdown 格式。";
            return new AiPrompt(original.task(), original.targetId(),
                    original.systemPrompt() + JSON_REPAIR_SYSTEM_SUFFIX, userPrompt);
        }

        // 2. 业务校验错误：追加字段修复提示
        String userPrompt = "原始任务要求：\n" + original.userPrompt()
                + "\n\n你之前的回复：\n" + malformedJson
                + "\n\n校验失败原因：" + validationError
                + "\n\n请修复业务字段后直接输出完整 JSON，不要包含任何解释或 markdown 格式。";
        return new AiPrompt(original.task(), original.targetId(),
                original.systemPrompt() + VALIDATION_REPAIR_SYSTEM_SUFFIX, userPrompt);
    }

    /** 对 Spring AI 结构化输出结果进行校验，校验失败返回 null */
    private <T> T validateStructured(AiPrompt prompt, T result, BiConsumer<T, AiPrompt> validator) {
        try {
            validator.accept(result, prompt);
            return result;
        } catch (IllegalArgumentException ex) {
            recordValidationFailure(prompt);
            return null;
        } finally {
            DefaultAiModelGateway.clearRequestContext();
        }
    }

    private String generateFromProvider(AiPrompt prompt) {
        return aiModelGateway.generateJson(prompt);
    }

    private void recordParseFailure(AiPrompt prompt) {
        var ctx = DefaultAiModelGateway.currentRequestContext();
        if (ctx != null) {
            aiMetrics.recordParseFailure(prompt.task(), ctx.provider(), ctx.model(), ctx.mode());
        } else {
            aiMetrics.recordParseFailure(prompt.task());
        }
    }

    private void recordValidationFailure(AiPrompt prompt) {
        var ctx = DefaultAiModelGateway.currentRequestContext();
        if (ctx != null) {
            aiMetrics.recordValidationFailure(prompt.task(), ctx.provider(), ctx.model(), ctx.mode());
        } else {
            aiMetrics.recordValidationFailure(prompt.task());
        }
    }

    private void recordTokenUsageIfPossible(AiPrompt prompt, String rawJson) {
        var ctx = DefaultAiModelGateway.currentRequestContext();
        if (ctx != null && rawJson != null) {
            int estimatedTokens = estimateTokens(prompt.systemPrompt(), prompt.userPrompt(), rawJson);
            aiMetrics.recordTokenUsage(prompt.task(), ctx.provider(), ctx.model(), estimatedTokens);
        }
    }

    /** 估算 token 用量（字符数 / 4） */
    private static int estimateTokens(String systemPrompt, String userPrompt, String response) {
        int charCount = (systemPrompt != null ? systemPrompt.length() : 0)
                + (userPrompt != null ? userPrompt.length() : 0)
                + (response != null ? response.length() : 0);
        return Math.max(1, charCount / 4);
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private void requireList(List<?> value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private void requireNonEmptyTextList(List<String> value, String field) {
        requireList(value, field);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        for (String item : value) {
            requireText(item, field + " item");
        }
    }

    /** 训练计划任务项 */
    public record TrainingPlanTaskItem(String title, String description, int dayIndex) {}
    /** 训练计划结果封装 */
    public record TrainingPlanResult(List<TrainingPlanTaskItem> tasks) {}

    /**
     * 生成 3 天训练计划，包含 6-12 个任务，每天 2-4 个。
     *
     * @param prompt AI 调用请求
     * @return 训练任务项列表
     */
    public List<TrainingPlanTaskItem> generateTrainingPlan(AiPrompt prompt) {
        TrainingPlanResult structuredResult = generateStructuredFromSpringProvider(prompt, TrainingPlanResult.class);
        if (structuredResult != null) {
            TrainingPlanResult validated = validateStructured(prompt, structuredResult, (r, p) -> validateTrainingPlan(r));
            if (validated != null) return validated.tasks();
        }
        TrainingPlanResult result = generateAndValidate(prompt, TrainingPlanResult.class, (r, p) -> validateTrainingPlan(r));
        return result.tasks();
    }

    private void validateTrainingPlan(TrainingPlanResult result) {
        if (result.tasks() == null || result.tasks().size() < 6 || result.tasks().size() > 12) {
            throw new IllegalArgumentException("Expected 6-12 training tasks (3 days, 2-4 per day)");
        }
        int[] dayCounts = new int[3];
        for (TrainingPlanTaskItem task : result.tasks()) {
            requireText(task.title(), "task.title");
            requireText(task.description(), "task.description");
            if (task.dayIndex() < 0 || task.dayIndex() > 2) {
                throw new IllegalArgumentException("task.dayIndex must be 0, 1, or 2");
            }
            dayCounts[task.dayIndex()]++;
        }
        for (int day = 0; day < 3; day++) {
            if (dayCounts[day] < 2 || dayCounts[day] > 4) {
                throw new IllegalArgumentException("Day " + day + " has " + dayCounts[day] + " tasks; expected 2-4 per day");
            }
        }
    }

    /**
     * 生成训练任务反馈，包含评分、问题诊断、改进示范和追问。
     *
     * @param prompt AI 调用请求
     * @return 训练反馈 DTO
     */
    public TrainingFeedbackDto generateTrainingFeedback(AiPrompt prompt) {
        TrainingFeedbackDto structuredResult = generateStructuredFromSpringProvider(prompt, TrainingFeedbackDto.class);
        if (structuredResult != null) {
            TrainingFeedbackDto validated = validateStructured(prompt, structuredResult, (dto, p) -> validateTrainingFeedback(dto));
            if (validated != null) return withBackendTaskId(validated, prompt.targetId());
        }
        TrainingFeedbackDto generated = generateAndValidate(
                prompt, TrainingFeedbackDto.class, (dto, p) -> validateTrainingFeedback(dto));
        return withBackendTaskId(generated, prompt.targetId());
    }

    /**
     * 生成自适应训练轮次结果，决定继续追问、通过或切换话题。
     *
     * @param prompt AI 调用请求
     * @return 自适应训练轮次 DTO
     */
    public AdaptiveTrainingTurnDto generateAdaptiveTrainingTurn(AiPrompt prompt) {
        AdaptiveTrainingTurnDto structuredResult = generateStructuredFromSpringProvider(prompt, AdaptiveTrainingTurnDto.class);
        if (structuredResult != null) {
            AdaptiveTrainingTurnDto validated = validateStructured(prompt, structuredResult, (dto, p) -> validateAdaptiveTrainingTurn(dto));
            if (validated != null) return validated;
        }
        return generateAndValidate(prompt, AdaptiveTrainingTurnDto.class, (dto, p) -> validateAdaptiveTrainingTurn(dto));
    }

    private void validateTrainingFeedback(TrainingFeedbackDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("TrainingFeedback is null");
        }
        if (dto.score() < 0 || dto.score() > 100) {
            throw new IllegalArgumentException("score out of range");
        }
        requireText(dto.feedback(), "feedback");
        requireText(dto.rewrittenAnswer(), "rewrittenAnswer");
        requireText(dto.followUpQuestion(), "followUpQuestion");
        requireList(dto.problems(), "problems");
        requireList(dto.recommendedReviewPoints(), "recommendedReviewPoints");
    }

    private TrainingFeedbackDto withBackendTaskId(TrainingFeedbackDto dto, String taskId) {
        return new TrainingFeedbackDto(
                taskId,
                dto.score(),
                dto.feedback(),
                dto.problems(),
                dto.rewrittenAnswer(),
                dto.followUpQuestion(),
                dto.recommendedReviewPoints());
    }

    private void validateAdaptiveTrainingTurn(AdaptiveTrainingTurnDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AdaptiveTrainingTurn is null");
        }
        if (!VALID_ADAPTIVE_TRAINING_ACTIONS.contains(dto.action())) {
            throw new IllegalArgumentException("invalid adaptive training action");
        }
        if (dto.score() < 0 || dto.score() > 100) {
            throw new IllegalArgumentException("score out of range");
        }
        requireText(dto.feedback(), "feedback");
        requireList(dto.problems(), "problems");
        requireList(dto.recommendedReviewPoints(), "recommendedReviewPoints");
        if ("continue".equals(dto.action()) || "switch".equals(dto.action())) {
            requireText(dto.nextQuestion(), "nextQuestion");
        }
        if ("pass".equals(dto.action()) || "stop".equals(dto.action())) {
            requireText(dto.summary(), "summary");
        }
    }

    /** 模拟面试问题封装 */
    public record MockInterviewQuestionResult(String question) {}

    /**
     * 生成模拟面试追问或开场问题。
     *
     * @param prompt AI 调用请求
     * @return 面试问题文本
     */
    public String generateMockInterviewQuestion(AiPrompt prompt) {
        MockInterviewQuestionResult structuredResult = generateStructuredFromSpringProvider(prompt, MockInterviewQuestionResult.class);
        if (structuredResult != null) {
            MockInterviewQuestionResult validated = validateStructured(prompt, structuredResult, (r, p) -> validateMockInterviewQuestion(r));
            if (validated != null) return validated.question();
        }
        MockInterviewQuestionResult result = generateAndValidate(prompt, MockInterviewQuestionResult.class, (r, p) -> validateMockInterviewQuestion(r));
        return result.question();
    }

    private void validateMockInterviewQuestion(MockInterviewQuestionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("MockInterviewQuestionResult is null");
        }
        requireText(result.question(), "question");
    }

    /**
     * 生成模拟面试报告，包含总分、维度评分、强弱项和改进示范。
     *
     * @param prompt AI 调用请求
     * @return 模拟面试报告 DTO
     */
    public MockInterviewReportDto generateMockInterviewReport(AiPrompt prompt) {
        MockInterviewReportDto structuredResult = generateStructuredFromSpringProvider(prompt, MockInterviewReportDto.class);
        if (structuredResult != null) {
            MockInterviewReportDto validated = validateStructured(prompt, structuredResult, (dto, p) -> validateMockInterviewReport(dto));
            if (validated != null) return validated;
        }
        return generateAndValidate(prompt, MockInterviewReportDto.class, (dto, p) -> validateMockInterviewReport(dto));
    }

    private void validateMockInterviewReport(MockInterviewReportDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("MockInterviewReport is null");
        }
        if (dto.overallScore() < 0 || dto.overallScore() > 100) {
            throw new IllegalArgumentException("overallScore out of range");
        }
        requireList(dto.dimensionScores(), "dimensionScores");
        requireText(dto.summary(), "summary");
        requireList(dto.strengths(), "strengths");
        requireList(dto.weaknesses(), "weaknesses");
        requireList(dto.improvedAnswers(), "improvedAnswers");
        requireList(dto.likelyFollowUpPoints(), "likelyFollowUpPoints");
        requireList(dto.nextTrainingTasks(), "nextTrainingTasks");
        for (DimensionScore dim : dto.dimensionScores()) {
            requireText(dim.name(), "dimension.name");
            if (dim.score() < 0 || dim.score() > 100) {
                throw new IllegalArgumentException("dimension score out of range");
            }
            requireText(dim.reason(), "dimension.reason");
        }
    }

    /**
     * 生成候选人简历摘要草稿。rawTextLength 由后端注入，不取自 AI 输出。
     *
     * @param prompt        AI 调用请求
     * @param rawTextLength 简历原文字符长度
     * @return 候选人简历摘要草稿 DTO
     */
    public CandidateProfileDraftDto generateCandidateProfileDraft(AiPrompt prompt, int rawTextLength) {
        CandidateProfileDraftDto structuredResult = generateStructuredFromSpringProvider(prompt, CandidateProfileDraftDto.class);
        CandidateProfileDraftDto aiResult = null;
        if (structuredResult != null) {
            aiResult = validateStructured(prompt, structuredResult, (dto, p) -> validateCandidateProfileDraft(dto));
        }
        if (aiResult == null) {
            aiResult = generateAndValidate(prompt, CandidateProfileDraftDto.class, (dto, p) -> validateCandidateProfileDraft(dto));
        }
        validateCandidateProfileDraft(aiResult);
        return new CandidateProfileDraftDto(
                aiResult.summary(),
                aiResult.skills() != null ? aiResult.skills() : List.of(),
                aiResult.projects() != null ? aiResult.projects() : List.of(),
                aiResult.experience() != null ? aiResult.experience() : List.of(),
                rawTextLength
        );
    }

    /**
     * 尝试通过 Spring AI 结构化输出直接映射实体，失败返回 null。
     *
     * @param prompt AI 调用请求
     * @param type   目标实体类型
     * @return 映射后的实体，或 null（映射失败时）
     * @param <T>   目标类型
     */
    private <T> T generateStructuredFromSpringProvider(AiPrompt prompt, Class<T> type) {
        try {
            // 1. 委托网关尝试 Spring AI 结构化输出
            return aiModelGateway.generateEntity(prompt, type);
        } catch (AiStructuredOutputMappingException ex) {
            // 2. 映射失败时记录解析失败指标，返回 null 由调用方回退
            recordParseFailure(prompt);
            DefaultAiModelGateway.clearRequestContext();
            return null;
        } catch (RuntimeException ex) {
            DefaultAiModelGateway.clearRequestContext();
            throw ex;
        }
    }

    private void validateCandidateProfileDraft(CandidateProfileDraftDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CandidateProfileDraft is null");
        }
        requireText(dto.summary(), "summary");
        requireList(dto.skills(), "skills");
        requireList(dto.projects(), "projects");
        requireList(dto.experience(), "experience");
    }

    /**
     * 生成教练记忆摘要，包含观察到的强弱项、待验证声明和训练重点。
     *
     * @param prompt AI 调用请求
     * @return 教练记忆 DTO
     */
    public CoachingMemoryDto generateCoachingMemory(AiPrompt prompt) {
        CoachingMemoryDto structuredResult = generateStructuredFromSpringProvider(prompt, CoachingMemoryDto.class);
        if (structuredResult != null) {
            CoachingMemoryDto validated = validateStructured(prompt, structuredResult, (dto, p) -> validateCoachingMemory(dto));
            if (validated != null) return validated;
        }
        return generateAndValidate(prompt, CoachingMemoryDto.class, (dto, p) -> validateCoachingMemory(dto));
    }

    private void validateCoachingMemory(CoachingMemoryDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CoachingMemory is null");
        }
        requireMemoryItems(dto.observedStrengths(), "observedStrengths");
        requireMemoryItems(dto.observedWeaknesses(), "observedWeaknesses");
        requireMemoryItems(dto.recurringProblems(), "recurringProblems");
        requireMemoryItems(dto.verifiedExperience(), "verifiedExperience");
        requireMemoryItems(dto.unverifiedClaims(), "unverifiedClaims");
        requireMemoryItems(dto.recommendedNextFocus(), "recommendedNextFocus");
        requireMemoryItems(dto.avoidRepeating(), "avoidRepeating");
    }

    private void requireMemoryItems(List<CoachingMemoryItemDto> items, String field) {
        requireList(items, field);
        for (CoachingMemoryItemDto item : items) {
            if (item == null) {
                throw new IllegalArgumentException(field + " contains null item");
            }
            requireText(item.content(), field + ".content");
            if (!VALID_MEMORY_SOURCES.contains(item.source())) {
                throw new IllegalArgumentException("invalid memory source: " + item.source());
            }
            if (!VALID_MEMORY_CONFIDENCE.contains(item.confidence())) {
                throw new IllegalArgumentException("invalid memory confidence: " + item.confidence());
            }
        }
    }

    private static final Set<String> VALID_AGENT_STAGES = Set.of(
            "targetSetup", "profileConfirmation", "assessment", "training", "mockInterview", "review");

    /**
     * 生成 Agent 决策，包含当前目标、关注维度、推荐动作和工具调用计划。
     *
     * @param prompt AI 调用请求
     * @return Agent 决策 DTO
     */
    public AgentDecisionDto generateAgentDecision(AiPrompt prompt) {
        AgentDecisionDto structuredResult = generateStructuredFromSpringProvider(prompt, AgentDecisionDto.class);
        if (structuredResult != null) {
            AgentDecisionDto validated = validateStructured(prompt, structuredResult, (dto, p) -> validateAgentDecision(dto));
            if (validated != null) return validated;
        }
        return generateAndValidate(prompt, AgentDecisionDto.class, (dto, p) -> validateAgentDecision(dto));
    }

    private void validateAgentDecision(AgentDecisionDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AgentDecision is null");
        }
        requireText(dto.currentGoal(), "currentGoal");
        requireList(dto.focusDimensions(), "focusDimensions");
        requireText(dto.recommendedAction(), "recommendedAction");
        requireText(dto.rationaleSummary(), "rationaleSummary");
        requireList(dto.toolCalls(), "toolCalls");
        for (AgentToolCallDto toolCall : dto.toolCalls()) {
            if (toolCall == null) {
                throw new IllegalArgumentException("toolCalls contains null item");
            }
            requireText(toolCall.toolName(), "toolCall.toolName");
            requireText(toolCall.reason(), "toolCall.reason");
        }
    }
}
