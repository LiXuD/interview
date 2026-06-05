package com.interviewcoach.coachingmemory.service;

import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.service.CoachEventService;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.coachingmemory.entity.CoachingMemory;
import com.interviewcoach.coachingmemory.entity.CoachingMemoryItem;
import com.interviewcoach.coachingmemory.repository.CoachingMemoryRepository;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentQuestionScoreDto;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.CoachingMemoryCorrectionRequest;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.CoachingMemoryItemDto;
import com.interviewcoach.common.api.MockInterviewReportDto;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.error.CoachingMemoryNotFoundException;
import com.interviewcoach.common.util.CollectionUtils;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 教练记忆业务服务，负责从测评、训练和模拟面试中生成结构化教练记忆，
 * 支持用户纠错和本地记忆导入。
 */
@Service
public class CoachingMemoryService {

    private static final Logger log = LoggerFactory.getLogger(CoachingMemoryService.class);
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是 AI 技术面试教练。根据候选人的%s生成结构化教练记忆。
            只返回合法 JSON 对象，不返回任何其他文字。

            JSON 结构必须严格如下：
            {
              "observedStrengths": [{"content": "强项描述", "source": "observed", "confidence": "high"}],
              "observedWeaknesses": [{"content": "短板描述", "source": "observed", "confidence": "medium"}],
              "recurringProblems": [{"content": "重复问题", "source": "observed", "confidence": "high"}],
              "verifiedExperience": [{"content": "已验证的经历", "source": "confirmed", "confidence": "high"}],
              "unverifiedClaims": [{"content": "未验证的声明", "source": "inferred", "confidence": "low"}],
              "recommendedNextFocus": [{"content": "下一步重点", "source": "observed", "confidence": "medium"}],
              "avoidRepeating": [{"content": "避免重复的内容", "source": "observed", "confidence": "medium"}]
            }

            所有字段必须是对象数组（允许空数组），每个对象必须包含 content、source、confidence。
            source 只能是 confirmed、observed、corrected、inferred、rejected。
            confidence 只能是 high、medium、low。
            inferred 只能表示需要追问验证的推断，不得写成事实；rejected 表示用户否认内容，后续不得再当事实使用。
            只基于本次%s的实际数据生成，不得编造候选人未提供的信息。
            """;

    private final AiStructuredOutputService aiService;
    private final CoachingMemoryRepository memoryRepository;
    private final InterviewTargetRepository targetRepository;
    private final CoachEventService coachEventService;

    public CoachingMemoryService(AiStructuredOutputService aiService,
                                 CoachingMemoryRepository memoryRepository,
                                 InterviewTargetRepository targetRepository,
                                 CoachEventService coachEventService) {
        this.aiService = aiService;
        this.memoryRepository = memoryRepository;
        this.targetRepository = targetRepository;
        this.coachEventService = coachEventService;
    }

    /**
     * 从测评结果生成教练记忆，沉淀候选人的能力观察。
     *
     * @param user           当前用户
     * @param targetId       目标岗位 ID
     * @param resultDto      测评结果
     * @param questions      测评题目列表
     * @param questionScores 逐题评分
     * @param sessionId      测评会话 ID
     * @return 生成的教练记忆 DTO
     */
    @Transactional
    public CoachingMemoryDto generateFromAssessment(User user, UUID targetId,
                                                    AssessmentResultDto resultDto,
                                                    List<AssessmentQuestionDto> questions,
                                                    List<AssessmentQuestionScoreDto> questionScores,
                                                    UUID sessionId) {
        InterviewTarget target = findTarget(targetId, user.getId());
        AiPrompt prompt = buildAssessmentPrompt(target, resultDto, questions, questionScores);
        return generateAndSave(user, target, "assessment", sessionId, prompt);
    }

    /**
     * 从训练反馈生成教练记忆，使用独立事务避免影响主流程。
     *
     * @param userId      用户 ID
     * @param targetId    目标岗位 ID
     * @param taskId      训练任务 ID
     * @param feedbackDto 训练反馈
     * @param taskTitle   任务标题
     * @return 生成的教练记忆 DTO
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CoachingMemoryDto generateFromTraining(UUID userId, UUID targetId,
                                                  UUID taskId,
                                                  TrainingFeedbackDto feedbackDto,
                                                  String taskTitle) {
        InterviewTarget target = findTarget(targetId, userId);
        AiPrompt prompt = buildTrainingPrompt(target, feedbackDto, taskTitle);
        return generateAndSave(target.getUser(), target, "training", taskId, prompt);
    }

    /**
     * 从模拟面试报告生成教练记忆，沉淀面试表现观察。
     *
     * @param user        当前用户
     * @param targetId    目标岗位 ID
     * @param reportDto   模拟面试报告
     * @param interviewId 模拟面试 ID
     * @return 生成的教练记忆 DTO
     */
    @Transactional
    public CoachingMemoryDto generateFromMockInterview(User user, UUID targetId,
                                                       MockInterviewReportDto reportDto,
                                                       UUID interviewId) {
        InterviewTarget target = findTarget(targetId, user.getId());
        AiPrompt prompt = buildMockInterviewPrompt(target, reportDto);
        return generateAndSave(user, target, "mockInterview", interviewId, prompt);
    }

    /**
     * 查询指定目标岗位下的所有教练记忆。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @return 教练记忆 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<CoachingMemoryDto> getMemories(UUID targetId, UUID userId) {
        return memoryRepository.findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
                .stream().map(this::toDto).toList();
    }

    /**
     * 查询单条教练记忆详情。
     *
     * @param memoryId 教练记忆 ID
     * @param userId   用户 ID
     * @return 教练记忆 DTO
     */
    @Transactional(readOnly = true)
    public CoachingMemoryDto getMemory(UUID memoryId, UUID userId) {
        CoachingMemory memory = memoryRepository.findByIdAndUserId(memoryId, userId)
                .orElseThrow(() -> new CoachingMemoryNotFoundException(memoryId));
        return toDto(memory);
    }

    /**
     * 用户纠正教练记忆中的某条记录，将来源标记为 corrected 或 rejected。
     *
     * @param memoryId 教练记忆 ID
     * @param userId   用户 ID
     * @param request  纠正请求，包含字段名、索引、新内容和来源
     * @return 更新后的教练记忆 DTO
     */
    @Transactional
    public CoachingMemoryDto correctMemoryItem(UUID memoryId, UUID userId, CoachingMemoryCorrectionRequest request) {
        // 第 1 步：校验记忆归属和纠正请求合法性（来源必须为 corrected 或 rejected）
        CoachingMemory memory = memoryRepository.findByIdAndUserId(memoryId, userId)
                .orElseThrow(() -> new CoachingMemoryNotFoundException(memoryId));
        validateCorrectionRequest(request);

        // 第 2 步：定位目标字段列表，校验索引范围
        List<CoachingMemoryItem> items = selectItemList(memory, request.field());
        if (request.itemIndex() < 0 || request.itemIndex() >= items.size()) {
            throw new IllegalArgumentException("Invalid coaching memory item index");
        }

        // 第 3 步：更新记忆项内容、来源和可信度
        CoachingMemoryItem item = items.get(request.itemIndex());
        item.setContent(request.content().trim());
        item.setSource(request.source());
        item.setConfidence("high");

        // 第 4 步：持久化并触发教练 Agent 事件
        CoachingMemoryDto result = toDto(memoryRepository.save(memory));

        fireAgentEvent(CoachEvent.MEMORY_CORRECTED, memory.getTarget().getId(), userId, memoryId, request);

        return result;
    }

    /**
     * 从本地教练记忆归档导入摘要，标记为 inferred 低可信度等待后续验证。
     *
     * @param user      当前用户
     * @param targetId  目标岗位 ID
     * @param summaries 本地记忆摘要列表
     * @return 导入后的教练记忆 DTO
     */
    @Transactional
    public CoachingMemoryDto importFromLocalArchive(User user, UUID targetId, List<String> summaries) {
        // 第 1 步：校验目标岗位归属
        InterviewTarget target = findTarget(targetId, user.getId());

        // 第 2 步：将本地摘要转为 inferred 低可信度记忆项
        List<CoachingMemoryItem> unverifiedItems = summaries.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(summary -> new CoachingMemoryItem(summary.trim(), "inferred", "low"))
                .toList();

        // 第 3 步：创建教练记忆实体，所有记忆项放至未验证分类
        CoachingMemory memory = new CoachingMemory();
        memory.setUser(user);
        memory.setTarget(target);
        memory.setSourceType("localArchiveImport");
        memory.setSourceId(UUID.randomUUID());
        memory.setObservedStrengths(List.of());
        memory.setObservedWeaknesses(List.of());
        memory.setRecurringProblems(List.of());
        memory.setVerifiedExperience(List.of());
        memory.setUnverifiedClaims(unverifiedItems);
        memory.setRecommendedNextFocus(List.of());
        memory.setAvoidRepeating(List.of());
        return toDto(memoryRepository.save(memory));
    }

    private InterviewTarget findTarget(UUID targetId, UUID userId) {
        return targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Target not found: " + targetId));
    }

    private void validateCorrectionRequest(CoachingMemoryCorrectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Correction request is required");
        }
        if (!"corrected".equals(request.source()) && !"rejected".equals(request.source())) {
            throw new IllegalArgumentException("Correction source must be corrected or rejected");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Correction content is required");
        }
    }

    private List<CoachingMemoryItem> selectItemList(CoachingMemory memory, String field) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("Correction field is required");
        }
        return switch (field) {
            case "observedStrengths" -> memory.getObservedStrengths();
            case "observedWeaknesses" -> memory.getObservedWeaknesses();
            case "recurringProblems" -> memory.getRecurringProblems();
            case "verifiedExperience" -> memory.getVerifiedExperience();
            case "unverifiedClaims" -> memory.getUnverifiedClaims();
            case "recommendedNextFocus" -> memory.getRecommendedNextFocus();
            case "avoidRepeating" -> memory.getAvoidRepeating();
            default -> throw new IllegalArgumentException("Unsupported coaching memory field: " + field);
        };
    }

    /**
     * 调用 AI 生成教练记忆并持久化，统一处理测评、训练和模拟面试三种来源。
     */
    private CoachingMemoryDto generateAndSave(User user, InterviewTarget target,
                                              String sourceType, UUID sourceId,
                                              AiPrompt prompt) {
        // 第 1 步：调用 AI 生成结构化教练记忆
        CoachingMemoryDto aiResult = aiService.generateCoachingMemory(prompt);

        // 第 2 步：将 AI 结果转为实体并设置来源信息
        CoachingMemory memory = new CoachingMemory();
        memory.setUser(user);
        memory.setTarget(target);
        memory.setSourceType(sourceType);
        memory.setSourceId(sourceId);
        memory.setObservedStrengths(toEntityItems(aiResult.observedStrengths()));
        memory.setObservedWeaknesses(toEntityItems(aiResult.observedWeaknesses()));
        memory.setRecurringProblems(toEntityItems(aiResult.recurringProblems()));
        memory.setVerifiedExperience(toEntityItems(aiResult.verifiedExperience()));
        memory.setUnverifiedClaims(toEntityItems(aiResult.unverifiedClaims()));
        memory.setRecommendedNextFocus(toEntityItems(aiResult.recommendedNextFocus()));
        memory.setAvoidRepeating(toEntityItems(aiResult.avoidRepeating()));

        // 第 3 步：持久化并返回 DTO
        return toDto(memoryRepository.save(memory));
    }

    private AiPrompt buildAssessmentPrompt(InterviewTarget target,
                                           AssessmentResultDto resultDto,
                                           List<AssessmentQuestionDto> questions,
                                           List<AssessmentQuestionScoreDto> questionScores) {
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted("测评结果", "测评");

        StringBuilder qaBuilder = new StringBuilder();
        qaBuilder.append("总分: %d\n\n".formatted(resultDto.totalScore()));
        qaBuilder.append("维度评分:\n");
        resultDto.dimensions().forEach(d ->
                qaBuilder.append("  - %s: %d 分, %s\n".formatted(d.name(), d.score(), d.reason())));
        qaBuilder.append("\n强项: %s\n".formatted(String.join("；", resultDto.strengths())));
        qaBuilder.append("短板: %s\n".formatted(String.join("；", resultDto.weaknesses())));
        qaBuilder.append("下一步: %s\n\n".formatted(String.join("；", resultDto.nextActions())));
        if (questions != null) {
            qaBuilder.append("题目概要:\n");
            for (int i = 0; i < questions.size(); i++) {
                AssessmentQuestionDto q = questions.get(i);
                qaBuilder.append("  Q%d [%s/%s]: %s\n".formatted(
                        i + 1, q.dimension(), q.difficulty(), q.question()));
            }
        }
        if (questionScores != null && !questionScores.isEmpty()) {
            qaBuilder.append("\n逐题诊断:\n");
            for (AssessmentQuestionScoreDto s : questionScores) {
                qaBuilder.append("  Q%d [%s] %d 分:\n".formatted(
                        s.questionIndex() + 1, s.dimension(), s.score()));
                qaBuilder.append("    反馈: %s\n".formatted(s.feedback()));
                appendListIfPresent(qaBuilder, "问题", s.problems());
                appendListIfPresent(qaBuilder, "追问风险", s.followUpRisks());
                appendListIfPresent(qaBuilder, "内容亮点", s.contentHighlights());
                appendListIfPresent(qaBuilder, "内容短板", s.contentGaps());
                if (s.answerStructure() != null) {
                    qaBuilder.append("    回答结构: 背景=%s, 任务=%s, 行动=%s, 结果=%s, 权衡=%s, 复盘=%s\n".formatted(
                            s.answerStructure().background(),
                            s.answerStructure().task(),
                            s.answerStructure().action(),
                            s.answerStructure().result(),
                            s.answerStructure().tradeoff(),
                            s.answerStructure().review()));
                }
            }
        }

        String userPrompt = """
                目标岗位：%s

                测评结果：
                %s
                """.formatted(target.getTitle(), qaBuilder);

        return new AiPrompt(AiPrompt.TASK_COACHING_MEMORY, target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildTrainingPrompt(InterviewTarget target,
                                         TrainingFeedbackDto feedbackDto,
                                         String taskTitle) {
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted("训练反馈", "训练");

        String userPrompt = """
                目标岗位：%s

                训练任务：%s

                评分：%d

                反馈：%s

                问题：%s

                改进示范：%s

                追问：%s

                建议复习：%s
                """.formatted(
                target.getTitle(),
                taskTitle != null ? taskTitle : "训练任务",
                feedbackDto.score(),
                feedbackDto.feedback(),
                String.join("；", feedbackDto.problems()),
                feedbackDto.rewrittenAnswer(),
                feedbackDto.followUpQuestion(),
                String.join("；", feedbackDto.recommendedReviewPoints())
        );

        return new AiPrompt(AiPrompt.TASK_COACHING_MEMORY, target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildMockInterviewPrompt(InterviewTarget target,
                                              MockInterviewReportDto reportDto) {
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted("模拟面试报告", "模拟面试");

        StringBuilder dimBuilder = new StringBuilder();
        reportDto.dimensionScores().forEach(d ->
                dimBuilder.append("  - %s: %d 分, %s\n".formatted(d.name(), d.score(), d.reason())));

        String userPrompt = """
                目标岗位：%s

                总体评分：%d

                总结：%s

                维度评分：
                %s
                强项：%s

                短板：%s

                改进示范：%s

                下一步训练：%s
                """.formatted(
                target.getTitle(),
                reportDto.overallScore(),
                reportDto.summary(),
                dimBuilder,
                String.join("；", reportDto.strengths()),
                String.join("；", reportDto.weaknesses()),
                String.join("；", reportDto.improvedAnswers()),
                String.join("；", reportDto.nextTrainingTasks())
        );

        return new AiPrompt(AiPrompt.TASK_COACHING_MEMORY, target.getId().toString(), systemPrompt, userPrompt);
    }

    private void appendListIfPresent(StringBuilder sb, String label, List<String> items) {
        if (items != null && !items.isEmpty()) {
            sb.append("    %s: %s\n".formatted(label, String.join("；", items)));
        }
    }

    private CoachingMemoryDto toDto(CoachingMemory memory) {
        return new CoachingMemoryDto(
                memory.getId().toString(),
                memory.getTarget().getId().toString(),
                memory.getSourceType(),
                memory.getSourceId().toString(),
                toDtoItems(memory.getObservedStrengths()),
                toDtoItems(memory.getObservedWeaknesses()),
                toDtoItems(memory.getRecurringProblems()),
                toDtoItems(memory.getVerifiedExperience()),
                toDtoItems(memory.getUnverifiedClaims()),
                toDtoItems(memory.getRecommendedNextFocus()),
                toDtoItems(memory.getAvoidRepeating()),
                memory.getCreatedAt().toString()
        );
    }

    private List<CoachingMemoryItem> toEntityItems(List<CoachingMemoryItemDto> items) {
        return CollectionUtils.copyList(items).stream()
                .map(item -> new CoachingMemoryItem(item.content(), item.source(), item.confidence()))
                .toList();
    }

    private List<CoachingMemoryItemDto> toDtoItems(List<CoachingMemoryItem> items) {
        return CollectionUtils.copyList(items).stream()
                .map(item -> new CoachingMemoryItemDto(item.getContent(), item.getSource(), item.getConfidence()))
                .toList();
    }

    private void fireAgentEvent(CoachEvent event,
                                UUID targetId,
                                UUID userId,
                                UUID sourceId,
                                CoachingMemoryCorrectionRequest request) {
        try {
            String discriminator = "%s:%s:%s:%d:%s:%s".formatted(
                    event.name(), sourceId, request.field(), request.itemIndex(), request.source(), request.content());
            coachEventService.recordEvent(userId, targetId, event, "coachingMemory", sourceId, discriminator);
        } catch (Exception ex) {
            log.warn("Agent event {} failed for targetId={}: {}", event.name(), targetId, ex.getMessage());
        }
    }
}
