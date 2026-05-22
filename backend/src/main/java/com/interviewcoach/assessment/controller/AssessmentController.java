package com.interviewcoach.assessment.controller;

import com.interviewcoach.assessment.service.AssessmentService;
import com.interviewcoach.common.api.AssessmentAnswerRequest;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.AssessmentSessionDto;
import com.interviewcoach.common.api.AssessmentStartRequest;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.OK)
    public AssessmentSessionDto start(@RequestBody AssessmentStartRequest request) {
        return assessmentService.startAssessment(SecurityUtils.currentUser(), UUID.fromString(request.targetId()));
    }

    @PostMapping("/{id}/answers")
    public AssessmentSessionDto submitAnswer(@PathVariable UUID id,
                                             @RequestBody AssessmentAnswerRequest request) {
        return assessmentService.submitAnswer(id, SecurityUtils.currentUser().getId(), request.answer());
    }

    @PostMapping("/{id}/finish")
    public AssessmentResultDto finish(@PathVariable UUID id) {
        return assessmentService.finishAssessment(id, SecurityUtils.currentUser().getId());
    }

    @GetMapping("/{id}")
    public AssessmentSessionDto get(@PathVariable UUID id) {
        return assessmentService.getAssessment(id, SecurityUtils.currentUser().getId());
    }
}
