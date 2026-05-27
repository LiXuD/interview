package com.interviewcoach.coachingmemory.service;

import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.coachingmemory.entity.CoachingMemory;
import com.interviewcoach.coachingmemory.entity.CoachingMemoryItem;
import com.interviewcoach.coachingmemory.repository.CoachingMemoryRepository;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.CoachingMemoryItemDto;
import com.interviewcoach.common.api.MockInterviewReportDto;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.error.CoachingMemoryNotFoundException;
import com.interviewcoach.common.util.CollectionUtils;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CoachingMemoryService {

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

    public CoachingMemoryService(AiStructuredOutputService aiService,
                                 CoachingMemoryRepository memoryRepository,
                                 InterviewTargetRepository targetRepository) {
        this.aiService = aiService;
        this.memoryRepository = memoryRepository;
        this.targetRepository = targetRepository;
    }

    @Transactional
    public CoachingMemoryDto generateFromAssessment(User user, UUID targetId,
                                                    AssessmentResultDto resultDto,
                                                    List<AssessmentQuestionDto> questions,
                                                    UUID sessionId) {
        InterviewTarget target = findTarget(targetId, user.getId());
        AiPrompt prompt = buildAssessmentPrompt(target, resultDto, questions);
        return generateAndSave(user, target, "assessment", sessionId, prompt);
    }

    @Transactional
    public CoachingMemoryDto generateFromTraining(User user, UUID targetId,
                                                  TrainingFeedbackDto feedbackDto,
                                                  String taskTitle) {
        InterviewTarget target = findTarget(targetId, user.getId());
        AiPrompt prompt = buildTrainingPrompt(target, feedbackDto, taskTitle);
        return generateAndSave(user, target, "training", UUID.fromString(feedbackDto.taskId()), prompt);
    }

    @Transactional
    public CoachingMemoryDto generateFromMockInterview(User user, UUID targetId,
                                                       MockInterviewReportDto reportDto,
                                                       UUID interviewId) {
        InterviewTarget target = findTarget(targetId, user.getId());
        AiPrompt prompt = buildMockInterviewPrompt(target, reportDto);
        return generateAndSave(user, target, "mockInterview", interviewId, prompt);
    }

    @Transactional(readOnly = true)
    public List<CoachingMemoryDto> getMemories(UUID targetId, UUID userId) {
        return memoryRepository.findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CoachingMemoryDto getMemory(UUID memoryId, UUID userId) {
        CoachingMemory memory = memoryRepository.findByIdAndUserId(memoryId, userId)
                .orElseThrow(() -> new CoachingMemoryNotFoundException(memoryId));
        return toDto(memory);
    }

    private InterviewTarget findTarget(UUID targetId, UUID userId) {
        return targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Target not found: " + targetId));
    }

    private CoachingMemoryDto generateAndSave(User user, InterviewTarget target,
                                              String sourceType, UUID sourceId,
                                              AiPrompt prompt) {
        CoachingMemoryDto aiResult = aiService.generateCoachingMemory(prompt);
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
        return toDto(memoryRepository.save(memory));
    }

    private AiPrompt buildAssessmentPrompt(InterviewTarget target,
                                           AssessmentResultDto resultDto,
                                           List<AssessmentQuestionDto> questions) {
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
}
