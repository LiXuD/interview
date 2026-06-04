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

@Component
public class AgentToolOrchestrator {

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

    private ToolResult actionReady(AgentToolCallDto toolCall, ToolContext context, String action) {
        return new ToolResult(toolCall.toolName(), "ready", Map.of(
                "targetId", context.targetId().toString(),
                "action", action
        ));
    }

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

    public record ToolContext(UUID targetId,
                              UUID userId,
                              ProgressDashboardDto progress,
                              List<CoachingMemoryDto> memories) {
    }

    public record ToolResult(String toolName, String status, Map<String, Object> summary) {
    }
}
