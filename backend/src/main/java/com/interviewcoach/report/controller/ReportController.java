package com.interviewcoach.report.controller;

import com.interviewcoach.common.api.ReportDto;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.report.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 报告控制器，提供查看测评报告和模拟面试报告的接口。
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 获取指定报告详情。
     *
     * @param id 报告 ID
     * @return 报告 DTO
     */
    @GetMapping("/{id}")
    public ReportDto get(@PathVariable UUID id) {
        return reportService.getReport(id, SecurityUtils.currentUser().getId());
    }

    /**
     * 获取指定目标岗位下所有报告列表。
     *
     * @param targetId 目标岗位 ID
     * @return 报告 DTO 列表，按创建时间倒序
     */
    @GetMapping
    public List<ReportDto> list(@RequestParam UUID targetId) {
        return reportService.listReports(targetId, SecurityUtils.currentUser().getId());
    }
}
