package com.interviewcoach.assessment.repository;

import com.interviewcoach.assessment.entity.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, UUID> {
    Optional<AssessmentSession> findByIdAndUserId(UUID id, UUID userId);
    void deleteByTargetId(UUID targetId);
    void deleteByUserId(UUID userId);
}
