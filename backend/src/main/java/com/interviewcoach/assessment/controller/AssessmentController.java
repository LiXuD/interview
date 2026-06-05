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

/**
 * 测评控制器，提供 5 题结构化测评的启动、答题、完成和查询接口。
 */
@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    /**
     * 启动一次新的测评，AI 生成 5 道结构化题目。
     *
     * @param request 包含目标岗位 ID 的请求体
     * @return 测评会话 DTO，包含题目列表
     */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.OK)
    public AssessmentSessionDto start(@RequestBody AssessmentStartRequest request) {
        return assessmentService.startAssessment(SecurityUtils.currentUser(), UUID.fromString(request.targetId()));
    }

    /**
     * 提交某道题的回答，AI 进行逐题评分。
     *
     * @param id      测评会话 ID
     * @param request 包含候选人回答的请求体
     * @return 更新后的测评会话 DTO
     */
    @PostMapping("/{id}/answers")
    public AssessmentSessionDto submitAnswer(@PathVariable UUID id,
                                             @RequestBody AssessmentAnswerRequest request) {
        return assessmentService.submitAnswer(id, SecurityUtils.currentUser().getId(), request.answer());
    }

    /**
     * 完成测评，AI 生成综合评分与报告。
     *
     * @param id 测评会话 ID
     * @return 测评结果 DTO，含总分、维度评分和改进建议
     */
    @PostMapping("/{id}/finish")
    public AssessmentResultDto finish(@PathVariable UUID id) {
        return assessmentService.finishAssessment(id, SecurityUtils.currentUser().getId());
    }

    /**
     * 查询测评会话详情。
     *
     * @param id 测评会话 ID
     * @return 测评会话 DTO
     */
    @GetMapping("/{id}")
    public AssessmentSessionDto get(@PathVariable UUID id) {
        return assessmentService.getAssessment(id, SecurityUtils.currentUser().getId());
    }
}
