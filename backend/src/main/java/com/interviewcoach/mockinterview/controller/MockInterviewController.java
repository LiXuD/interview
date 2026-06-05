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

/**
 * 模拟面试 REST 控制器，提供面试的开始、回答、结束和查询接口。
 */
@RestController
@RequestMapping("/api/mock-interviews")
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    public MockInterviewController(MockInterviewService mockInterviewService) {
        this.mockInterviewService = mockInterviewService;
    }

    /**
     * 开始一次新的模拟面试，AI 生成开场问题。
     *
     * @param request 包含目标岗位 ID 和可选的侧重维度
     * @return 面试会话 DTO，含首题
     */
    @PostMapping("/start")
    public MockInterviewSessionDto start(@RequestBody MockInterviewStartRequest request) {
        return mockInterviewService.startInterview(
                SecurityUtils.currentUser(),
                UUID.fromString(request.targetId()),
                request.focusDimension());
    }

    /**
     * 查询指定目标岗位下的所有模拟面试列表。
     *
     * @param targetId 目标岗位 ID
     * @return 面试会话列表
     */
    @GetMapping("/target/{targetId}")
    public List<MockInterviewSessionDto> listByTarget(@PathVariable UUID targetId) {
        return mockInterviewService.listInterviews(targetId, SecurityUtils.currentUser().getId());
    }

    /**
     * 提交用户回答，AI 基于上下文生成追问。
     *
     * @param id      面试会话 ID
     * @param request 包含用户回答内容
     * @return 更新后的面试会话 DTO，含下一个问题
     */
    @PostMapping("/{id}/answer")
    public MockInterviewSessionDto answer(@PathVariable UUID id,
                                          @RequestBody MockInterviewAnswerRequest request) {
        return mockInterviewService.submitAnswer(
                id,
                SecurityUtils.currentUser().getId(),
                request.answer());
    }

    /**
     * 结束模拟面试，AI 生成复盘报告。
     *
     * @param id 面试会话 ID
     * @return 模拟面试复盘报告
     */
    @PostMapping("/{id}/finish")
    public MockInterviewReportDto finish(@PathVariable UUID id) {
        return mockInterviewService.finishInterview(
                id,
                SecurityUtils.currentUser().getId());
    }

    /**
     * 获取单个模拟面试会话详情。
     *
     * @param id 面试会话 ID
     * @return 面试会话 DTO
     */
    @GetMapping("/{id}")
    public MockInterviewSessionDto get(@PathVariable UUID id) {
        return mockInterviewService.getInterview(
                id,
                SecurityUtils.currentUser().getId());
    }
}
