package com.interviewcoach.training.repository;

import com.interviewcoach.training.entity.TrainingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 训练计划数据访问接口。
 */
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {
    /** 按目标岗位和用户查询训练计划 */
    Optional<TrainingPlan> findByTargetIdAndUserId(UUID targetId, UUID userId);
    /** 删除指定目标岗位下的所有训练计划 */
    void deleteByTargetId(UUID targetId);
    /** 删除指定用户的所有训练计划 */
    void deleteByUserId(UUID userId);
}
