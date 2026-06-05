package com.interviewcoach.assessment.repository;

import com.interviewcoach.assessment.entity.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 测评会话数据访问接口。
 */
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, UUID> {
    /** 按会话 ID 和用户查询，确保用户只能访问自己的测评 */
    Optional<AssessmentSession> findByIdAndUserId(UUID id, UUID userId);
    /** 删除指定目标岗位下的所有测评会话 */
    void deleteByTargetId(UUID targetId);
    /** 删除指定用户的所有测评会话 */
    void deleteByUserId(UUID userId);
}
