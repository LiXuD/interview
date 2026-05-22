package com.interviewcoach.training.controller;

import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.api.TrainingTaskAnswerRequest;
import com.interviewcoach.common.api.TrainingTaskDto;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.training.service.TrainingService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/training-tasks")
public class TrainingTaskController {

    private final TrainingService trainingService;

    public TrainingTaskController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/{id}/answer")
    public TrainingFeedbackDto submitAnswer(@PathVariable UUID id,
                                            @RequestBody TrainingTaskAnswerRequest request) {
        return trainingService.submitAnswer(id, SecurityUtils.currentUser().getId(), request.answer());
    }

    @PatchMapping("/{id}/complete")
    public TrainingTaskDto completeTask(@PathVariable UUID id) {
        return trainingService.completeTask(id, SecurityUtils.currentUser().getId());
    }
}
