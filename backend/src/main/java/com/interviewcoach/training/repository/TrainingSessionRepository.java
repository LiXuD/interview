package com.interviewcoach.training.repository;

import com.interviewcoach.training.entity.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {
    Optional<TrainingSession> findByIdAndTaskPlanUserId(UUID id, UUID userId);
}
