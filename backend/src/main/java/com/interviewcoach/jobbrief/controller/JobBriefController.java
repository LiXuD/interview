package com.interviewcoach.jobbrief.controller;

import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.JobBriefGenerateRequest;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.jobbrief.service.JobBriefService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 岗位画像控制器，提供岗位画像的生成与查询接口。
 */
@RestController
@RequestMapping("/api/job-briefs")
public class JobBriefController {

    private final JobBriefService jobBriefService;

    public JobBriefController(JobBriefService jobBriefService) {
        this.jobBriefService = jobBriefService;
    }

    /**
     * 根据 JD 和候选人摘要生成岗位画像，包含技能匹配、面试主题和风险分析。
     *
     * @param request 包含目标岗位 ID 的请求体
     * @return 岗位画像 DTO
     */
    @PostMapping("/generate")
    public JobBriefDto generate(@RequestBody JobBriefGenerateRequest request) {
        return jobBriefService.generate(SecurityUtils.currentUser(), request);
    }

    /**
     * 查询指定目标岗位的岗位画像。
     *
     * @param targetId 目标岗位 ID
     * @return 岗位画像 DTO
     */
    @GetMapping("/{targetId}")
    public JobBriefDto getByTargetId(@PathVariable UUID targetId) {
        return jobBriefService.getByTargetId(targetId, SecurityUtils.currentUser().getId());
    }
}
