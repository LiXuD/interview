package com.interviewcoach.agent;

import com.interviewcoach.agent.service.AgentToolOrchestrator;
import com.interviewcoach.ai.service.AiMetrics;
import com.interviewcoach.common.api.AgentToolCallDto;
import com.interviewcoach.common.api.ProgressDashboardDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolOrchestratorTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AgentToolOrchestrator orchestrator = new AgentToolOrchestrator(new AiMetrics(meterRegistry));

    @Test
    void analyzeProgressReturnsSanitizedSummaryAndRecordsMetrics() {
        UUID targetId = UUID.randomUUID();
        ProgressDashboardDto progress = new ProgressDashboardDto(
                targetId.toString(),
                82,
                List.of(),
                new ProgressDashboardDto.TrainingCompletionDto(9, 3, 0.33),
                List.of(new ProgressDashboardDto.DimensionSummaryDto("systemThinking", 80, "up")),
                List.of("系统设计表达不够结构化")
        );

        List<AgentToolOrchestrator.ToolResult> results = orchestrator.execute(
                List.of(new AgentToolCallDto("analyzeProgress", "读取能力趋势")),
                new AgentToolOrchestrator.ToolContext(targetId, UUID.randomUUID(), progress, List.of())
        );

        assertEquals(1, results.size());
        AgentToolOrchestrator.ToolResult result = results.get(0);
        assertEquals("analyzeProgress", result.toolName());
        assertEquals("completed", result.status());
        assertEquals(targetId.toString(), result.summary().get("targetId"));
        assertEquals(1, result.summary().get("dimensionCount"));
        assertEquals(1, result.summary().get("recentWeaknessCount"));
        assertEquals(Boolean.TRUE, result.summary().get("hasLatestAssessment"));
        assertEquals(1.0, meterRegistry.get("agent.tool.total")
                .tag("toolName", "analyzeProgress")
                .tag("outcome", "success")
                .counter()
                .count());
    }

    @Test
    void unknownToolFailsAndRecordsFailureMetric() {
        UUID targetId = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> orchestrator.execute(
                List.of(new AgentToolCallDto("unknownTool", "非法工具")),
                new AgentToolOrchestrator.ToolContext(targetId, UUID.randomUUID(), null, List.of())
        ));

        assertEquals(1.0, meterRegistry.get("agent.tool.total")
                .tag("toolName", "unknownTool")
                .tag("outcome", "failure")
                .counter()
                .count());
    }
}
