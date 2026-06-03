package com.interviewcoach.agent.service;

import com.interviewcoach.agent.entity.InterviewCoachAgent;
import com.interviewcoach.agent.repository.AgentRepository;
import com.interviewcoach.common.api.CoachAgentDto;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final InterviewTargetRepository targetRepository;

    public AgentService(AgentRepository agentRepository,
                        InterviewTargetRepository targetRepository) {
        this.agentRepository = agentRepository;
        this.targetRepository = targetRepository;
    }

    @Transactional
    public CoachAgentDto getOrCreateByTarget(UUID targetId, UUID userId) {
        InterviewCoachAgent agent = agentRepository.findByTargetIdAndUserId(targetId, userId)
                .orElseGet(() -> createNewAgent(targetId, userId));
        return toDto(agent);
    }

    @Transactional(readOnly = true)
    public CoachAgentDto getByTarget(UUID targetId, UUID userId) {
        InterviewCoachAgent agent = agentRepository.findByTargetIdAndUserId(targetId, userId)
                .orElse(null);
        return agent != null ? toDto(agent) : null;
    }

    private InterviewCoachAgent createNewAgent(UUID targetId, UUID userId) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Target not found: " + targetId));
        InterviewCoachAgent agent = new InterviewCoachAgent();
        agent.setUser(target.getUser());
        agent.setTarget(target);
        return agentRepository.save(agent);
    }

    private CoachAgentDto toDto(InterviewCoachAgent agent) {
        return new CoachAgentDto(
                agent.getId().toString(),
                agent.getTarget().getId().toString(),
                agent.getStatus(),
                agent.getCurrentStage(),
                agent.getCurrentGoal(),
                agent.getActiveFocusDimensions() != null ? new ArrayList<>(agent.getActiveFocusDimensions()) : List.of(),
                agent.getNextRecommendedAction(),
                agent.getLastEventType(),
                agent.getLastDecisionSummary(),
                agent.getLastRunAt() != null ? agent.getLastRunAt().toString() : null,
                agent.getCreatedAt().toString(),
                agent.getUpdatedAt().toString()
        );
    }
}
