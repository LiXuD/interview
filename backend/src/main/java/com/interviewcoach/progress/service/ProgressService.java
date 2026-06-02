package com.interviewcoach.progress.service;

import com.interviewcoach.assessment.entity.AssessmentResult;
import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.assessment.service.DimensionAnalysisService;
import com.interviewcoach.common.api.DimensionAnalysisDto;
import com.interviewcoach.common.api.DimensionDetailDto;
import com.interviewcoach.common.api.ProgressDashboardDto;
import com.interviewcoach.coachingmemory.entity.CoachingMemory;
import com.interviewcoach.coachingmemory.repository.CoachingMemoryRepository;
import com.interviewcoach.report.entity.Report;
import com.interviewcoach.report.repository.ReportRepository;
import com.interviewcoach.training.entity.TrainingPlan;
import com.interviewcoach.training.entity.TrainingTask;
import com.interviewcoach.training.repository.TrainingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProgressService {

    private final AssessmentResultRepository assessmentResultRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final ReportRepository reportRepository;
    private final CoachingMemoryRepository coachingMemoryRepository;
    private final DimensionAnalysisService dimensionAnalysisService;

    public ProgressService(AssessmentResultRepository assessmentResultRepository,
                           TrainingPlanRepository trainingPlanRepository,
                           ReportRepository reportRepository,
                           CoachingMemoryRepository coachingMemoryRepository,
                           DimensionAnalysisService dimensionAnalysisService) {
        this.assessmentResultRepository = assessmentResultRepository;
        this.trainingPlanRepository = trainingPlanRepository;
        this.reportRepository = reportRepository;
        this.coachingMemoryRepository = coachingMemoryRepository;
        this.dimensionAnalysisService = dimensionAnalysisService;
    }

    @Transactional(readOnly = true)
    public ProgressDashboardDto getDashboard(UUID targetId, UUID userId) {
        // Score trend from assessments
        List<AssessmentResult> assessmentResults = assessmentResultRepository
                .findBySessionTargetIdAndSessionUserIdOrderByCreatedAtDesc(targetId, userId);

        Integer latestScore = assessmentResults.isEmpty() ? null : assessmentResults.get(0).getTotalScore();

        List<ProgressDashboardDto.ScoreTrendEntry> scoreTrend = new ArrayList<>();
        for (AssessmentResult result : assessmentResults) {
            scoreTrend.add(new ProgressDashboardDto.ScoreTrendEntry(
                    result.getTotalScore(), "assessment", result.getCreatedAt().toString()));
        }

        // Add mock interview scores from reports
        List<Report> mockInterviewReports = reportRepository
                .findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
                .stream()
                .filter(r -> "mockInterview".equals(r.getType()))
                .toList();
        for (Report report : mockInterviewReports) {
            try {
                var content = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        report.getContent(), java.util.Map.class);
                Object scoreObj = content.get("overallScore");
                if (scoreObj instanceof Number score) {
                    scoreTrend.add(new ProgressDashboardDto.ScoreTrendEntry(
                            score.intValue(), "mockInterview", report.getCreatedAt().toString()));
                }
            } catch (Exception ignored) {
            }
        }

        // Sort by date
        scoreTrend.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));

        // Training completion
        ProgressDashboardDto.TrainingCompletionDto trainingCompletion = trainingPlanRepository
                .findByTargetIdAndUserId(targetId, userId)
                .map(this::calculateCompletion)
                .orElse(new ProgressDashboardDto.TrainingCompletionDto(0, 0, 0.0));

        // Dimension summary
        DimensionAnalysisDto dimensionAnalysis = dimensionAnalysisService.analyze(targetId, userId);
        List<ProgressDashboardDto.DimensionSummaryDto> dimensionSummary = dimensionAnalysis.dimensions().stream()
                .map(d -> new ProgressDashboardDto.DimensionSummaryDto(d.name(), d.latestScore(), d.trend()))
                .toList();

        // Recent weaknesses from coaching memory
        List<String> recentWeaknesses = coachingMemoryRepository
                .findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
                .stream()
                .flatMap(m -> m.getObservedWeaknesses().stream())
                .map(item -> item.getContent())
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .limit(5)
                .toList();

        return new ProgressDashboardDto(
                targetId.toString(), latestScore, scoreTrend,
                trainingCompletion, dimensionSummary, recentWeaknesses);
    }

    private ProgressDashboardDto.TrainingCompletionDto calculateCompletion(TrainingPlan plan) {
        int total = plan.getTasks().size();
        int completed = (int) plan.getTasks().stream()
                .filter(t -> "completed".equals(t.getStatus()))
                .count();
        double rate = total > 0 ? (double) completed / total : 0.0;
        return new ProgressDashboardDto.TrainingCompletionDto(total, completed, rate);
    }
}
