package com.interviewcoach.agent.controller;

import com.interviewcoach.agent.service.AgentService;
import com.interviewcoach.common.api.CoachAgentDto;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 教练 Agent REST 控制器。提供查询指定目标岗位的教练 Agent 状态接口。
 */
@RestController
@RequestMapping("/api/targets/{targetId}/coach-agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 获取指定目标岗位的教练 Agent 状态，不存在时自动创建。
     *
     * @param targetId 目标岗位 ID（路径参数）
     * @return 教练 Agent DTO
     */
    @GetMapping
    public CoachAgentDto getAgent(@PathVariable UUID targetId) {
        return agentService.getOrCreateByTarget(targetId, SecurityUtils.currentUser().getId());
    }
}
