package com.interviewcoach.assessment.repository;

import com.interviewcoach.assessment.entity.AssessmentResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, UUID> {
    List<AssessmentResult> findBySessionTargetIdAndSessionUserIdOrderByCreatedAtDesc(UUID targetId, UUID userId);
    void deleteBySessionTargetId(UUID targetId);
    void deleteBySessionUserId(UUID userId);
}
