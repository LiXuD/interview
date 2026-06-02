package com.interviewcoach.progress.controller;

import com.interviewcoach.common.api.ProgressDashboardDto;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.progress.service.ProgressService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    public ProgressDashboardDto getDashboard(@RequestParam UUID targetId) {
        return progressService.getDashboard(targetId, SecurityUtils.currentUser().getId());
    }
}
