package com.interviewcoach.assessment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.assessment.entity.AssessmentDimension;
import com.interviewcoach.assessment.entity.AssessmentResult;
import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.coachingmemory.entity.CoachingMemory;
import com.interviewcoach.coachingmemory.entity.CoachingMemoryItem;
import com.interviewcoach.coachingmemory.repository.CoachingMemoryRepository;
import com.interviewcoach.common.api.DimensionAnalysisDto;
import com.interviewcoach.common.api.DimensionDetailDto;
import com.interviewcoach.report.entity.Report;
import com.interviewcoach.report.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DimensionAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DimensionAnalysisService.class);

    private static final List<String> FIXED_DIMENSIONS = List.of(
            "technicalDepth", "projectSpecificity", "systemThinking",
            "tradeoffAwareness", "failureHandling", "communicationClarity", "businessContext"
    );

    private final AssessmentResultRepository assessmentResultRepository;
    private final ReportRepository reportRepository;
    private final CoachingMemoryRepository coachingMemoryRepository;
    private final ObjectMapper objectMapper;

    public DimensionAnalysisService(AssessmentResultRepository assessmentResultRepository,
                                    ReportRepository reportRepository,
                                    CoachingMemoryRepository coachingMemoryRepository,
                                    ObjectMapper objectMapper) {
        this.assessmentResultRepository = assessmentResultRepository;
        this.reportRepository = reportRepository;
        this.coachingMemoryRepository = coachingMemoryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DimensionAnalysisDto analyze(UUID targetId, UUID userId) {
        List<DimensionDetailDto> details = new ArrayList<>();

        List<AssessmentResult> assessmentResults = assessmentResultRepository
                .findBySessionTargetIdAndSessionUserIdOrderByCreatedAtDesc(targetId, userId);

        List<Report> mockInterviewReports = reportRepository
                .findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
                .stream()
                .filter(r -> "mockInterview".equals(r.getType()))
                .toList();

        List<CoachingMemory> memories = coachingMemoryRepository
                .findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId);

        for (String dimension : FIXED_DIMENSIONS) {
            details.add(analyzeDimension(dimension, assessmentResults, mockInterviewReports, memories));
        }

        return new DimensionAnalysisDto(targetId.toString(), details);
    }

    private DimensionDetailDto analyzeDimension(String dimensionName,
                                                 List<AssessmentResult> assessmentResults,
                                                 List<Report> mockInterviewReports,
                                                 List<CoachingMemory> memories) {
        List<DimensionDetailDto.DimensionScoreEntry> scoreHistory = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> evidenceSources = new ArrayList<>();
        List<String> nextFocus = new ArrayList<>();

        // Collect scores from assessment results
        for (AssessmentResult result : assessmentResults) {
            for (AssessmentDimension d : result.getDimensions()) {
                if (dimensionName.equals(d.getName())) {
                    scoreHistory.add(new DimensionDetailDto.DimensionScoreEntry(
                            d.getScore(), "assessment", result.getCreatedAt().toString()));
                    if (d.getReason() != null && !d.getReason().isBlank()) {
                        weaknesses.add(d.getReason());
                    }
                    break;
                }
            }
        }

        // Collect scores from mock interview reports
        for (Report report : mockInterviewReports) {
            extractDimensionFromReport(report, dimensionName, scoreHistory, weaknesses);
        }

        // Collect evidence from coaching memory
        for (CoachingMemory memory : memories) {
            extractFromCoachingMemory(memory, dimensionName, weaknesses, evidenceSources, nextFocus);
        }

        // Sort by createdAt descending so index 0 is the global latest across all sources
        scoreHistory.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));

        // Calculate latest score and trend (scoreHistory is in descending createdAt order)
        Integer latestScore = scoreHistory.isEmpty() ? null : scoreHistory.get(0).score();
        String trend = calculateTrend(scoreHistory);

        // Deduplicate and limit lists
        weaknesses = weaknesses.stream().distinct().limit(5).toList();
        evidenceSources = evidenceSources.stream().distinct().limit(5).toList();
        nextFocus = nextFocus.stream().distinct().limit(3).toList();

        return new DimensionDetailDto(
                dimensionName, latestScore, trend, scoreHistory,
                weaknesses, evidenceSources, nextFocus);
    }

    private void extractDimensionFromReport(Report report, String dimensionName,
                                            List<DimensionDetailDto.DimensionScoreEntry> scoreHistory,
                                            List<String> weaknesses) {
        try {
            Map<String, Object> content = objectMapper.readValue(report.getContent(),
                    new TypeReference<>() {});
            Object dimScoresObj = content.get("dimensionScores");
            if (dimScoresObj instanceof List<?> dimScores) {
                for (Object dimObj : dimScores) {
                    if (dimObj instanceof Map<?, ?> dim) {
                        String name = (String) dim.get("name");
                        if (dimensionName.equals(name)) {
                            Object scoreObj = dim.get("score");
                            if (scoreObj instanceof Number score) {
                                scoreHistory.add(new DimensionDetailDto.DimensionScoreEntry(
                                        score.intValue(), "mockInterview", report.getCreatedAt().toString()));
                            }
                            String reason = (String) dim.get("reason");
                            if (reason != null && !reason.isBlank()) {
                                weaknesses.add(reason);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse mock interview report {} for dimension analysis", report.getId(), e);
        }
    }

    private void extractFromCoachingMemory(CoachingMemory memory, String dimensionName,
                                           List<String> weaknesses,
                                           List<String> evidenceSources,
                                           List<String> nextFocus) {
        // Check observed weaknesses
        for (CoachingMemoryItem item : memory.getObservedWeaknesses()) {
            if (item.getContent() != null && containsDimension(item.getContent(), dimensionName)) {
                weaknesses.add(item.getContent());
                evidenceSources.add("coachingMemory:observed");
            }
        }
        // Check recurring problems
        for (CoachingMemoryItem item : memory.getRecurringProblems()) {
            if (item.getContent() != null && containsDimension(item.getContent(), dimensionName)) {
                weaknesses.add(item.getContent());
                evidenceSources.add("coachingMemory:recurring");
            }
        }
        // Check recommended next focus
        for (CoachingMemoryItem item : memory.getRecommendedNextFocus()) {
            if (item.getContent() != null) {
                nextFocus.add(item.getContent());
                evidenceSources.add("coachingMemory:recommended");
            }
        }
    }

    private boolean containsDimension(String content, String dimensionName) {
        String lower = content.toLowerCase();
        return switch (dimensionName) {
            case "technicalDepth" -> lower.contains("技术深度") || lower.contains("technical");
            case "projectSpecificity" -> lower.contains("项目") || lower.contains("project");
            case "systemThinking" -> lower.contains("系统") || lower.contains("架构") || lower.contains("system");
            case "tradeoffAwareness" -> lower.contains("权衡") || lower.contains("tradeoff") || lower.contains("取舍");
            case "failureHandling" -> lower.contains("故障") || lower.contains("异常") || lower.contains("failure");
            case "communicationClarity" -> lower.contains("表达") || lower.contains("沟通") || lower.contains("communication");
            case "businessContext" -> lower.contains("业务") || lower.contains("business");
            default -> false;
        };
    }

    private String calculateTrend(List<DimensionDetailDto.DimensionScoreEntry> scoreHistory) {
        if (scoreHistory.size() < 2) return "insufficient_data";
        // scoreHistory is in descending createdAt order: index 0 = latest, index 1 = previous
        int latest = scoreHistory.get(0).score();
        int previous = scoreHistory.get(1).score();
        int diff = latest - previous;
        if (diff > 5) return "improving";
        if (diff < -5) return "declining";
        return "stable";
    }
}
