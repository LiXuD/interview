package com.interviewcoach.agent.repository;

import com.interviewcoach.agent.entity.InterviewCoachAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 教练 Agent 数据访问接口。按目标岗位和用户查询、删除 Agent 实例。
 */
public interface AgentRepository extends JpaRepository<InterviewCoachAgent, UUID> {

    /** 按目标岗位 ID 和用户 ID 查询 Agent */
    Optional<InterviewCoachAgent> findByTargetIdAndUserId(UUID targetId, UUID userId);

    /** 按 Agent ID 和用户 ID 查询，确保归属校验 */
    Optional<InterviewCoachAgent> findByIdAndUserId(UUID id, UUID userId);

    /** 删除用户的所有 Agent */
    void deleteByUserId(UUID userId);

    /** 删除目标岗位对应的 Agent */
    void deleteByTargetId(UUID targetId);
}
