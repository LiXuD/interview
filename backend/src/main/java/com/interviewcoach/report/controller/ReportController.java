package com.interviewcoach.report.controller;

import com.interviewcoach.common.api.ReportDto;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.report.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{id}")
    public ReportDto get(@PathVariable UUID id) {
        return reportService.getReport(id, SecurityUtils.currentUser().getId());
    }

    @GetMapping
    public List<ReportDto> list(@RequestParam UUID targetId) {
        return reportService.listReports(targetId, SecurityUtils.currentUser().getId());
    }
}
