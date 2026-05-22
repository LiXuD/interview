package com.interviewcoach.jobbrief.controller;

import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.JobBriefGenerateRequest;
import com.interviewcoach.jobbrief.service.JobBriefService;
import com.interviewcoach.user.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/job-briefs")
public class JobBriefController {

    private final JobBriefService jobBriefService;

    public JobBriefController(JobBriefService jobBriefService) {
        this.jobBriefService = jobBriefService;
    }

    @PostMapping("/generate")
    public JobBriefDto generate(@RequestBody JobBriefGenerateRequest request) {
        return jobBriefService.generate(currentUser(), request);
    }

    @GetMapping("/{targetId}")
    public JobBriefDto getByTargetId(@PathVariable UUID targetId) {
        return jobBriefService.getByTargetId(targetId, currentUser().getId());
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
