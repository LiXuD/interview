package com.interviewcoach.training.controller;

import com.interviewcoach.common.api.AdaptiveTrainingAnswerRequest;
import com.interviewcoach.common.api.AdaptiveTrainingSessionDto;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.training.service.TrainingService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/training-sessions")
public class TrainingSessionController {

    private final TrainingService trainingService;

    public TrainingSessionController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/{id}/answers")
    public AdaptiveTrainingSessionDto submitAnswer(@PathVariable UUID id,
                                                   @RequestBody AdaptiveTrainingAnswerRequest request) {
        return trainingService.submitAdaptiveAnswer(id, SecurityUtils.currentUser().getId(), request.answer());
    }
}
