package com.interviewcoach.agent.repository;

import com.interviewcoach.agent.entity.InterviewCoachAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<InterviewCoachAgent, UUID> {
    Optional<InterviewCoachAgent> findByTargetIdAndUserId(UUID targetId, UUID userId);
    Optional<InterviewCoachAgent> findByIdAndUserId(UUID id, UUID userId);
    void deleteByUserId(UUID userId);
    void deleteByTargetId(UUID targetId);
}
