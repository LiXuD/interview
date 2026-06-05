package com.interviewcoach.assessment.controller;

import com.interviewcoach.assessment.service.DimensionAnalysisService;
import com.interviewcoach.common.api.DimensionAnalysisDto;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 能力维度分析控制器，汇总测评、模拟面试和教练记忆中的维度评分趋势。
 */
@RestController
@RequestMapping("/api/dimension-analysis")
public class DimensionAnalysisController {

    private final DimensionAnalysisService analysisService;

    public DimensionAnalysisController(DimensionAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 分析指定目标岗位下 7 个能力维度的评分历史、趋势和短板。
     *
     * @param targetId 目标岗位 ID
     * @return 维度分析 DTO，含各维度的评分历史、趋势和改进建议
     */
    @GetMapping
    public DimensionAnalysisDto analyze(@RequestParam UUID targetId) {
        return analysisService.analyze(targetId, SecurityUtils.currentUser().getId());
    }
}
