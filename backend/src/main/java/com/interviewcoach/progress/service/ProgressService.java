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

/**
 * 教练进步追踪服务，聚合测评分数趋势、训练完成率、能力维度和近期短板。
 */
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

    /**
     * 获取指定目标岗位的进步追踪 Dashboard 数据。
     * 聚合测评分数趋势、模拟面试分数、训练完成率、能力维度分析和近期短板。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @return Dashboard DTO
     */
    @Transactional(readOnly = true)
    public ProgressDashboardDto getDashboard(UUID targetId, UUID userId) {
        // 1. 查询测评结果，构建测评分数趋势
        List<AssessmentResult> assessmentResults = assessmentResultRepository
                .findBySessionTargetIdAndSessionUserIdOrderByCreatedAtDesc(targetId, userId);

        Integer latestScore = assessmentResults.isEmpty() ? null : assessmentResults.get(0).getTotalScore();

        List<ProgressDashboardDto.ScoreTrendEntry> scoreTrend = new ArrayList<>();
        for (AssessmentResult result : assessmentResults) {
            scoreTrend.add(new ProgressDashboardDto.ScoreTrendEntry(
                    result.getTotalScore(), "assessment", result.getCreatedAt().toString()));
        }

        // 2. 从模拟面试报告中提取 overallScore，追加到分数趋势
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
                // 报告内容解析失败时跳过该条目
            }
        }

        // 3. 按时间倒序排列分数趋势
        scoreTrend.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));

        // 4. 计算训练计划完成率
        ProgressDashboardDto.TrainingCompletionDto trainingCompletion = trainingPlanRepository
                .findByTargetIdAndUserId(targetId, userId)
                .map(this::calculateCompletion)
                .orElse(new ProgressDashboardDto.TrainingCompletionDto(0, 0, 0.0));

        // 5. 查询能力维度分析
        DimensionAnalysisDto dimensionAnalysis = dimensionAnalysisService.analyze(targetId, userId);
        List<ProgressDashboardDto.DimensionSummaryDto> dimensionSummary = dimensionAnalysis.dimensions().stream()
                .map(d -> new ProgressDashboardDto.DimensionSummaryDto(d.name(), d.latestScore(), d.trend()))
                .toList();

        // 6. 从教练记忆中提取近期短板（去重取前 5 条）
        List<String> recentWeaknesses = coachingMemoryRepository
                .findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
                .stream()
                .flatMap(m -> m.getObservedWeaknesses().stream())
                .map(item -> item.getContent())
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .limit(5)
                .toList();

        // 7. 组装并返回 Dashboard DTO
        return new ProgressDashboardDto(
                targetId.toString(), latestScore, scoreTrend,
                trainingCompletion, dimensionSummary, recentWeaknesses);
    }

    /**
     * 计算训练计划完成率。
     *
     * @param plan 训练计划实体
     * @return 训练完成 DTO，包含总任务数、已完成数和完成率
     */
    private ProgressDashboardDto.TrainingCompletionDto calculateCompletion(TrainingPlan plan) {
        int total = plan.getTasks().size();
        int completed = (int) plan.getTasks().stream()
                .filter(t -> "completed".equals(t.getStatus()))
                .count();
        double rate = total > 0 ? (double) completed / total : 0.0;
        return new ProgressDashboardDto.TrainingCompletionDto(total, completed, rate);
    }
}
