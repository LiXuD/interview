package com.interviewcoach.training.repository;

import com.interviewcoach.training.entity.TrainingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainingTaskRepository extends JpaRepository<TrainingTask, UUID> {
    Optional<TrainingTask> findByIdAndPlanUserId(UUID id, UUID userId);
}
