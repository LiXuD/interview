package com.interviewcoach.training.controller;

import com.interviewcoach.common.api.AdaptiveTrainingAnswerRequest;
import com.interviewcoach.common.api.AdaptiveTrainingSessionDto;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.training.service.TrainingService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 自适应训练会话控制器，处理训练会话中的回答提交。
 */
@RestController
@RequestMapping("/api/training-sessions")
public class TrainingSessionController {

    private final TrainingService trainingService;

    public TrainingSessionController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    /**
     * 向自适应训练会话提交回答，AI 根据回答决定追问或结束。
     *
     * @param id      训练会话 ID
     * @param request 包含候选人回答的请求体
     * @return 更新后的自适应训练会话 DTO
     */
    @PostMapping("/{id}/answers")
    public AdaptiveTrainingSessionDto submitAnswer(@PathVariable UUID id,
                                                   @RequestBody AdaptiveTrainingAnswerRequest request) {
        return trainingService.submitAdaptiveAnswer(id, SecurityUtils.currentUser().getId(), request.answer());
    }
}
