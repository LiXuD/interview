package com.interviewcoach.training.controller;

import com.interviewcoach.common.api.TrainingPlanDto;
import com.interviewcoach.common.api.TrainingPlanGenerateRequest;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.training.service.TrainingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 训练计划控制器，提供训练计划的生成与查询接口。
 */
@RestController
@RequestMapping("/api/training-plans")
public class TrainingPlanController {

    private final TrainingService trainingService;

    public TrainingPlanController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    /**
     * 根据目标岗位的测评结果生成训练计划。
     *
     * @param request 包含目标岗位 ID 的请求体
     * @return 生成的训练计划 DTO
     */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.OK)
    public TrainingPlanDto generatePlan(@RequestBody TrainingPlanGenerateRequest request) {
        return trainingService.generatePlan(SecurityUtils.currentUser(), UUID.fromString(request.targetId()));
    }

    /**
     * 查询指定目标岗位的训练计划。
     *
     * @param targetId 目标岗位 ID
     * @return 训练计划 DTO
     */
    @GetMapping("/{targetId}")
    public TrainingPlanDto getPlan(@PathVariable UUID targetId) {
        return trainingService.getPlan(targetId, SecurityUtils.currentUser().getId());
    }
}
