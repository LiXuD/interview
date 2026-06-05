package com.interviewcoach.progress.controller;

import com.interviewcoach.common.api.ProgressDashboardDto;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.progress.service.ProgressService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 教练进步追踪控制器，提供 Dashboard 数据查询接口。
 */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * 获取指定目标岗位的进步追踪 Dashboard 数据。
     *
     * @param targetId 目标岗位 ID
     * @return Dashboard DTO，包含分数趋势、训练完成率、能力维度和近期短板
     */
    @GetMapping
    public ProgressDashboardDto getDashboard(@RequestParam UUID targetId) {
        return progressService.getDashboard(targetId, SecurityUtils.currentUser().getId());
    }
}
