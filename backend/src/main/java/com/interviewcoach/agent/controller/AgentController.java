package com.interviewcoach.agent.controller;

import com.interviewcoach.agent.service.AgentService;
import com.interviewcoach.common.api.CoachAgentDto;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/targets/{targetId}/coach-agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public CoachAgentDto getAgent(@PathVariable UUID targetId) {
        return agentService.getOrCreateByTarget(targetId, SecurityUtils.currentUser().getId());
    }
}
