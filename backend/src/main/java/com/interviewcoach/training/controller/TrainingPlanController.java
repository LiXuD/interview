package com.interviewcoach.training.controller;

import com.interviewcoach.common.api.TrainingPlanDto;
import com.interviewcoach.common.api.TrainingPlanGenerateRequest;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.training.service.TrainingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/training-plans")
public class TrainingPlanController {

    private final TrainingService trainingService;

    public TrainingPlanController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.OK)
    public TrainingPlanDto generatePlan(@RequestBody TrainingPlanGenerateRequest request) {
        return trainingService.generatePlan(SecurityUtils.currentUser(), UUID.fromString(request.targetId()));
    }

    @GetMapping("/{targetId}")
    public TrainingPlanDto getPlan(@PathVariable UUID targetId) {
        return trainingService.getPlan(targetId, SecurityUtils.currentUser().getId());
    }
}
