package com.interviewcoach.training.repository;

import com.interviewcoach.training.entity.TrainingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 训练任务数据访问接口。
 */
public interface TrainingTaskRepository extends JpaRepository<TrainingTask, UUID> {
    /** 按任务 ID 和所属用户查询，确保用户只能访问自己的任务 */
    Optional<TrainingTask> findByIdAndPlanUserId(UUID id, UUID userId);
}
