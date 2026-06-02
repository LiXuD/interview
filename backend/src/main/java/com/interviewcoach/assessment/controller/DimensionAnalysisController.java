package com.interviewcoach.assessment.controller;

import com.interviewcoach.assessment.service.DimensionAnalysisService;
import com.interviewcoach.common.api.DimensionAnalysisDto;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dimension-analysis")
public class DimensionAnalysisController {

    private final DimensionAnalysisService analysisService;

    public DimensionAnalysisController(DimensionAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping
    public DimensionAnalysisDto analyze(@RequestParam UUID targetId) {
        return analysisService.analyze(targetId, SecurityUtils.currentUser().getId());
    }
}
