package com.interviewcoach.agent.service;

import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.entity.CoachEventRecord;
import com.interviewcoach.agent.entity.InterviewCoachAgent;
import com.interviewcoach.agent.repository.AgentRepository;
import com.interviewcoach.ai.service.AiMetrics;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.common.api.AgentDecisionDto;
import com.interviewcoach.common.api.AgentToolCallDto;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.CoachingMemoryItemDto;
import com.interviewcoach.common.api.ProgressDashboardDto;
import com.interviewcoach.coachingmemory.service.CoachingMemoryService;
import com.interviewcoach.progress.service.ProgressService;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InterviewCoachAgentRunner {

    private static final Logger log = LoggerFactory.getLogger(InterviewCoachAgentRunner.class);

    public static final Set<String> ALLOWED_TOOLS = Set.of(
            "startAssessment",
            "generateTrainingPlan",
            "startAdaptiveTraining",
            "startMockInterview",
            "analyzeProgress",
            "updateCoachingMemory"
    );

    private static final int MAX_MODEL_CALLS = 1;
    private static final int MAX_TOOL_CALLS = 3;

    private final AgentRepository agentRepository;
    private final InterviewTargetRepository targetRepository;
    private final AiStructuredOutputService aiService;
    private final CoachingMemoryService coachingMemoryService;
    private final ProgressService progressService;
    private final AiMetrics aiMetrics;
    private final CoachEventService coachEventService;

    public InterviewCoachAgentRunner(AgentRepository agentRepository,
                                     InterviewTargetRepository targetRepository,
                                     AiStructuredOutputService aiService,
                                     CoachingMemoryService coachingMemoryService,
                                     ProgressService progressService,
                                     AiMetrics aiMetrics,
                                     CoachEventService coachEventService) {
        this.agentRepository = agentRepository;
        this.targetRepository = targetRepository;
        this.aiService = aiService;
        this.coachingMemoryService = coachingMemoryService;
        this.progressService = progressService;
        this.aiMetrics = aiMetrics;
        this.coachEventService = coachEventService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void run(UUID eventId) {
        CoachEventRecord event = coachEventService.claim(eventId);
        if (event == null) {
            return;
        }
        try {
            AgentDecisionDto decision = handleEvent(
                    CoachEvent.valueOf(event.getEventType()), event.getTargetId(), event.getUserId());
            if (decision == null) {
                coachEventService.markFailed(eventId, "AgentDecisionFailed");
                return;
            }
            coachEventService.markCompleted(eventId);
        } catch (Exception ex) {
            coachEventService.markFailed(eventId, ex.getClass().getSimpleName());
        }
    }

    @Transactional
    public AgentDecisionDto handleEvent(CoachEvent event, UUID targetId, UUID userId) {
        long startNanos = aiMetrics.startTimerNanos();
        try {
            InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Target not found: " + targetId));

            InterviewCoachAgent agent = agentRepository.findByTargetIdAndUserId(targetId, userId)
                    .orElseGet(() -> {
                        InterviewCoachAgent newAgent = new InterviewCoachAgent();
                        newAgent.setUser(target.getUser());
                        newAgent.setTarget(target);
                        return agentRepository.save(newAgent);
            });

            String newStage = determineStage(event, agent.getCurrentStage());

            ProgressDashboardDto progress = loadProgress(targetId, userId);
            List<CoachingMemoryDto> memories = coachingMemoryService.getMemories(targetId, userId);

            AiPrompt prompt = buildPrompt(agent, target, event, progress, memories);
            AgentDecisionDto decision = aiService.generateAgentDecision(prompt);

            validateToolCalls(decision);
            enforceToolCallBudget(decision);

            agent.setCurrentStage(newStage);
            agent.setLastEventType(event.name());
            agent.setCurrentGoal(decision.currentGoal());
            agent.setActiveFocusDimensions(new ArrayList<>(decision.focusDimensions()));
            agent.setNextRecommendedAction(decision.recommendedAction());
            agent.setLastDecisionSummary(decision.rationaleSummary());
            agent.setLastRunAt(Instant.now());
            agentRepository.save(agent);

            recordAgentMetrics(startNanos, event.name(), newStage, "success");
            log.info("Agent event={} targetId={} stage={} tools={}",
                    event.name(), targetId, newStage, decision.toolCalls().size());
            return decision;
        } catch (Exception ex) {
            recordAgentMetrics(startNanos, event.name(), null, "failure");
            log.warn("Agent event {} failed for targetId={}: {}", event.name(), targetId, ex.getMessage());
            return null;
        }
    }

    private String determineStage(CoachEvent event, String currentStage) {
        return switch (event) {
            case TARGET_CREATED -> "targetSetup";
            case RESUME_SUMMARY_CONFIRMED -> "profileConfirmation";
            case ASSESSMENT_COMPLETED -> "assessment";
            case TRAINING_TASK_COMPLETED, TRAINING_SESSION_COMPLETED -> "training";
            case MOCK_INTERVIEW_COMPLETED -> "mockInterview";
            case MEMORY_CORRECTED -> currentStage;
            case APP_SESSION_STARTED -> currentStage;
        };
    }

    private ProgressDashboardDto loadProgress(UUID targetId, UUID userId) {
        try {
            return progressService.getDashboard(targetId, userId);
        } catch (Exception ex) {
            log.warn("Failed to load progress for targetId={}: {}", targetId, ex.getMessage());
            return null;
        }
    }

    private AiPrompt buildPrompt(InterviewCoachAgent agent,
                                  InterviewTarget target,
                                  CoachEvent event,
                                  ProgressDashboardDto progress,
                                  List<CoachingMemoryDto> memories) {
        String systemPrompt = """
                你是 AI 技术面试教练 Agent。你持续跟踪候选人的面试准备进度，并在每个关键事件后做出下一步决策。
                你只能从白名单工具中选择动作。只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {
                  "currentGoal": "当前教练目标",
                  "focusDimensions": ["维度1", "维度2"],
                  "recommendedAction": "推荐的下一步行动",
                  "rationaleSummary": "简短决策理由",
                  "toolCalls": [{"toolName": "工具名", "reason": "调用原因"}],
                  "memoryUpdateRequired": false,
                  "planAdjustmentRequired": false
                }

                白名单工具：%s

                约束：
                - toolCalls 中的 toolName 必须是白名单工具之一。
                - focusDimensions 列出当前优先改善的能力维度，最多 3 个。
                - rationaleSummary 是可展示给用户的简短理由，不要输出思维链。
                - 只基于提供的上下文做决策，不要编造候选人未提供的信息。
                """.formatted(String.join(", ", ALLOWED_TOOLS));

        StringBuilder userBuilder = new StringBuilder();
        userBuilder.append("目标岗位：%s\n".formatted(target.getTitle()));
        userBuilder.append("触发事件：%s\n".formatted(event.name()));
        userBuilder.append("当前阶段：%s\n".formatted(agent.getCurrentStage()));
        if (agent.getCurrentGoal() != null) {
            userBuilder.append("当前目标：%s\n".formatted(agent.getCurrentGoal()));
        }
        if (agent.getActiveFocusDimensions() != null && !agent.getActiveFocusDimensions().isEmpty()) {
            userBuilder.append("当前关注维度：%s\n".formatted(String.join(", ", agent.getActiveFocusDimensions())));
        }
        if (agent.getLastDecisionSummary() != null) {
            userBuilder.append("上次决策摘要：%s\n".formatted(agent.getLastDecisionSummary()));
        }

        if (progress != null) {
            userBuilder.append("\n进度概览：\n");
            if (progress.latestAssessmentScore() != null) {
                userBuilder.append("  最新测评分数：%d\n".formatted(progress.latestAssessmentScore()));
            }
            if (progress.trainingCompletion() != null) {
                userBuilder.append("  训练完成：%d/%d (%.0f%%)\n".formatted(
                        progress.trainingCompletion().completedTasks(),
                        progress.trainingCompletion().totalTasks(),
                        progress.trainingCompletion().completionRate() * 100));
            }
            if (progress.dimensionSummary() != null && !progress.dimensionSummary().isEmpty()) {
                userBuilder.append("  维度概览：\n");
                for (ProgressDashboardDto.DimensionSummaryDto dim : progress.dimensionSummary()) {
                    String score = dim.latestScore() != null ? String.valueOf(dim.latestScore()) : "无";
                    userBuilder.append("    - %s: %s 分, 趋势=%s\n".formatted(dim.name(), score, dim.trend()));
                }
            }
            if (progress.recentWeaknesses() != null && !progress.recentWeaknesses().isEmpty()) {
                userBuilder.append("  近期短板：%s\n".formatted(String.join("；", progress.recentWeaknesses())));
            }
        }

        if (memories != null && !memories.isEmpty()) {
            CoachingMemoryDto latest = memories.get(0);
            userBuilder.append("\n教练记忆（最新）：\n");
            appendMemoryItems(userBuilder, "强项", latest.observedStrengths());
            appendMemoryItems(userBuilder, "短板", latest.observedWeaknesses());
            appendMemoryItems(userBuilder, "待验证", latest.unverifiedClaims());
            appendMemoryItems(userBuilder, "建议重点", latest.recommendedNextFocus());
        }

        return new AiPrompt(AiPrompt.TASK_AGENT_DECISION, target.getId().toString(), systemPrompt, userBuilder.toString());
    }

    private void appendMemoryItems(StringBuilder sb, String label, List<CoachingMemoryItemDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<CoachingMemoryItemDto> visibleItems = items.stream()
                .filter(item -> isVisibleMemoryItem(label, item))
                .toList();
        if (visibleItems.isEmpty()) {
            return;
        }

        sb.append("  %s：".formatted(label));
        visibleItems.forEach(item ->
                sb.append("[%s/%s] %s; ".formatted(item.source(), item.confidence(), item.content())));
        sb.append("\n");
    }

    private boolean isVisibleMemoryItem(String label, CoachingMemoryItemDto item) {
        if (item == null || "rejected".equals(item.source())) {
            return false;
        }
        return !"inferred".equals(item.source()) || "待验证".equals(label);
    }

    private void validateToolCalls(AgentDecisionDto decision) {
        for (AgentToolCallDto toolCall : decision.toolCalls()) {
            if (!ALLOWED_TOOLS.contains(toolCall.toolName())) {
                throw new IllegalStateException(
                        "Agent requested non-whitelisted tool: " + toolCall.toolName());
            }
        }
    }

    private void enforceToolCallBudget(AgentDecisionDto decision) {
        if (decision.toolCalls().size() > MAX_TOOL_CALLS) {
            throw new IllegalStateException(
                    "Agent requested %d tool calls, budget is %d".formatted(
                            decision.toolCalls().size(), MAX_TOOL_CALLS));
        }
    }

    private void recordAgentMetrics(long startNanos, String event, String stage, String outcome) {
        long durationNanos = System.nanoTime() - startNanos;
        io.micrometer.core.instrument.Timer.builder("agent.event.duration")
                .description("Agent event processing latency")
                .tag("event", event)
                .tag("outcome", outcome)
                .register(aiMetrics.meterRegistry())
                .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        io.micrometer.core.instrument.Counter.builder("agent.event.total")
                .description("Total agent events")
                .tag("event", event)
                .tag("outcome", outcome)
                .register(aiMetrics.meterRegistry())
                .increment();
    }
}
