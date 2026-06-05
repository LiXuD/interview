package com.interviewcoach.training.repository;

import com.interviewcoach.training.entity.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 自适应训练会话数据访问接口。
 */
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {
    /** 按会话 ID 和所属用户查询，确保用户只能访问自己的会话 */
    Optional<TrainingSession> findByIdAndTaskPlanUserId(UUID id, UUID userId);
}
