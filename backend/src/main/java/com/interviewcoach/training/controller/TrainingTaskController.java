package com.interviewcoach.training.controller;

import com.interviewcoach.common.api.AdaptiveTrainingSessionDto;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.api.TrainingTaskAnswerRequest;
import com.interviewcoach.common.api.TrainingTaskDto;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.training.service.TrainingService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 训练任务控制器，提供任务回答提交、自适应训练启动和任务完成接口。
 */
@RestController
@RequestMapping("/api/training-tasks")
public class TrainingTaskController {

    private final TrainingService trainingService;

    public TrainingTaskController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    /**
     * 提交训练任务的回答，AI 进行评分并返回反馈。
     *
     * @param id      训练任务 ID
     * @param request 包含候选人回答的请求体
     * @return AI 生成的训练反馈 DTO
     */
    @PostMapping("/{id}/answer")
    public TrainingFeedbackDto submitAnswer(@PathVariable UUID id,
                                            @RequestBody TrainingTaskAnswerRequest request) {
        return trainingService.submitAnswer(id, SecurityUtils.currentUser().getId(), request.answer());
    }

    /**
     * 启动训练任务的自适应训练会话，AI 围绕短板进行多轮追问。
     *
     * @param id 训练任务 ID
     * @return 新建的自适应训练会话 DTO
     */
    @PostMapping("/{id}/adaptive-sessions/start")
    public AdaptiveTrainingSessionDto startAdaptiveSession(@PathVariable UUID id) {
        return trainingService.startAdaptiveSession(id, SecurityUtils.currentUser().getId());
    }

    /**
     * 标记训练任务为已完成状态。
     *
     * @param id 训练任务 ID
     * @return 更新后的训练任务 DTO
     */
    @PatchMapping("/{id}/complete")
    public TrainingTaskDto completeTask(@PathVariable UUID id) {
        return trainingService.completeTask(id, SecurityUtils.currentUser().getId());
    }
}
