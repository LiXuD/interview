package com.interviewcoach.mockinterview.controller;

import com.interviewcoach.common.api.MockInterviewAnswerRequest;
import com.interviewcoach.common.api.MockInterviewReportDto;
import com.interviewcoach.common.api.MockInterviewSessionDto;
import com.interviewcoach.common.api.MockInterviewStartRequest;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.mockinterview.service.MockInterviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/mock-interviews")
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    public MockInterviewController(MockInterviewService mockInterviewService) {
        this.mockInterviewService = mockInterviewService;
    }

    @PostMapping("/start")
    public MockInterviewSessionDto start(@RequestBody MockInterviewStartRequest request) {
        return mockInterviewService.startInterview(
                SecurityUtils.currentUser(),
                UUID.fromString(request.targetId()),
                request.focusDimension());
    }

    @GetMapping("/target/{targetId}")
    public List<MockInterviewSessionDto> listByTarget(@PathVariable UUID targetId) {
        return mockInterviewService.listInterviews(targetId, SecurityUtils.currentUser().getId());
    }

    @PostMapping("/{id}/answer")
    public MockInterviewSessionDto answer(@PathVariable UUID id,
                                          @RequestBody MockInterviewAnswerRequest request) {
        return mockInterviewService.submitAnswer(
                id,
                SecurityUtils.currentUser().getId(),
                request.answer());
    }

    @PostMapping("/{id}/finish")
    public MockInterviewReportDto finish(@PathVariable UUID id) {
        return mockInterviewService.finishInterview(
                id,
                SecurityUtils.currentUser().getId());
    }

    @GetMapping("/{id}")
    public MockInterviewSessionDto get(@PathVariable UUID id) {
        return mockInterviewService.getInterview(
                id,
                SecurityUtils.currentUser().getId());
    }
}
