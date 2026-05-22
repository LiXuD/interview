package com.interviewcoach.training.repository;

import com.interviewcoach.training.entity.TrainingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {
    Optional<TrainingPlan> findByTargetIdAndUserId(UUID targetId, UUID userId);
    void deleteByTargetId(UUID targetId);
    void deleteByUserId(UUID userId);
}
