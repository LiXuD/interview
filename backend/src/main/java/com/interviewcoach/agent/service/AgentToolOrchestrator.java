package com.interviewcoach.agent.service;

import com.interviewcoach.ai.service.AiMetrics;
import com.interviewcoach.common.api.AgentToolCallDto;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.ProgressDashboardDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Agent 工具编排器。执行 AI 决策中指定的工具调用，
 * 包括启动测评、生成训练计划、开始模拟面试、分析进度和更新教练记忆等。
 * <p>所有工具调用受白名单约束，不接受未注册的工具名。</p>
 */
@Component
public class AgentToolOrchestrator {

    /** 已注册的 Agent 工具白名单 */
    public static final Set<String> REGISTERED_TOOLS = Set.of(
            "startAssessment",
            "generateTrainingPlan",
            "startAdaptiveTraining",
            "startMockInterview",
            "analyzeProgress",
            "updateCoachingMemory"
    );

    private final AiMetrics aiMetrics;

    public AgentToolOrchestrator(AiMetrics aiMetrics) {
        this.aiMetrics = aiMetrics;
    }

    /**
     * 批量执行工具调用。
     *
     * @param toolCalls AI 决策中的工具调用列表
     * @param context   工具执行上下文（包含目标岗位、用户、进度和记忆）
     * @return 每个工具调用的执行结果
     */
    public List<ToolResult> execute(List<AgentToolCallDto> toolCalls, ToolContext context) {
        return toolCalls.stream()
                .map(toolCall -> execute(toolCall, context))
                .toList();
    }

    private ToolResult execute(AgentToolCallDto toolCall, ToolContext context) {
        long startNanos = System.nanoTime();
        String outcome = "success";
        try {
            if (!REGISTERED_TOOLS.contains(toolCall.toolName())) {
                throw new IllegalStateException("Agent requested unregistered tool: " + toolCall.toolName());
            }
            return switch (toolCall.toolName()) {
                case "startAssessment" -> actionReady(toolCall, context, "assessment");
                case "generateTrainingPlan" -> actionReady(toolCall, context, "trainingPlan");
                case "startAdaptiveTraining" -> actionReady(toolCall, context, "adaptiveTraining");
                case "startMockInterview" -> actionReady(toolCall, context, "mockInterview");
                case "analyzeProgress" -> analyzeProgress(toolCall, context);
                case "updateCoachingMemory" -> summarizeMemory(toolCall, context);
                default -> throw new IllegalStateException("Unhandled agent tool: " + toolCall.toolName());
            };
        } catch (RuntimeException ex) {
            outcome = "failure";
            throw ex;
        } finally {
            recordToolMetrics(toolCall.toolName(), outcome, startNanos);
        }
    }

    /** 标记动作就绪，返回 ready 状态和目标信息 */
    private ToolResult actionReady(AgentToolCallDto toolCall, ToolContext context, String action) {
        return new ToolResult(toolCall.toolName(), "ready", Map.of(
                "targetId", context.targetId().toString(),
                "action", action
        ));
    }

    /**
     * 分析用户训练进度，返回维度数量和近期短板。
     *
     * @param toolCall 工具调用请求
     * @param context  工具执行上下文
     * @return 包含进度统计的工具结果
     */
    private ToolResult analyzeProgress(AgentToolCallDto toolCall, ToolContext context) {
        ProgressDashboardDto progress = context.progress();
        if (progress == null) {
            return new ToolResult(toolCall.toolName(), "unavailable", Map.of(
                    "targetId", context.targetId().toString()
            ));
        }
        int dimensionCount = progress.dimensionSummary() == null ? 0 : progress.dimensionSummary().size();
        int weaknessCount = progress.recentWeaknesses() == null ? 0 : progress.recentWeaknesses().size();
        return new ToolResult(toolCall.toolName(), "completed", Map.of(
                "targetId", context.targetId().toString(),
                "dimensionCount", dimensionCount,
                "recentWeaknessCount", weaknessCount,
                "hasLatestAssessment", progress.latestAssessmentScore() != null
        ));
    }

    /**
     * 汇总教练记忆概况，包括记忆总数和是否存在可信记忆。
     *
     * @param toolCall 工具调用请求
     * @param context  工具执行上下文
     * @return 包含记忆统计的工具结果
     */
    private ToolResult summarizeMemory(AgentToolCallDto toolCall, ToolContext context) {
        List<CoachingMemoryDto> memories = context.memories();
        int memoryCount = memories == null ? 0 : memories.size();
        boolean hasTrustedMemory = memories != null && memories.stream().anyMatch(this::hasTrustedMemory);
        return new ToolResult(toolCall.toolName(), "completed", Map.of(
                "targetId", context.targetId().toString(),
                "memoryCount", memoryCount,
                "hasTrustedMemory", hasTrustedMemory
        ));
    }

    private boolean hasTrustedMemory(CoachingMemoryDto memory) {
        return hasItems(memory.observedStrengths())
                || hasItems(memory.observedWeaknesses())
                || hasItems(memory.recurringProblems())
                || hasItems(memory.verifiedExperience())
                || hasItems(memory.recommendedNextFocus());
    }

    private boolean hasItems(List<?> items) {
        return items != null && !items.isEmpty();
    }

    /** 记录工具调用耗时和次数指标 */
    private void recordToolMetrics(String toolName, String outcome, long startNanos) {
        long durationNanos = System.nanoTime() - startNanos;
        Timer.builder("agent.tool.duration")
                .description("Agent tool orchestration latency")
                .tag("toolName", toolName)
                .tag("outcome", outcome)
                .register(aiMetrics.meterRegistry())
                .record(durationNanos, TimeUnit.NANOSECONDS);
        Counter.builder("agent.tool.total")
                .description("Agent tool orchestration count")
                .tag("toolName", toolName)
                .tag("outcome", outcome)
                .register(aiMetrics.meterRegistry())
                .increment();
    }

    /** 工具执行上下文 */
    public record ToolContext(UUID targetId,
                              UUID userId,
                              ProgressDashboardDto progress,
                              List<CoachingMemoryDto> memories) {
    }

    /** 工具执行结果 */
    public record ToolResult(String toolName, String status, Map<String, Object> summary) {
    }
}
