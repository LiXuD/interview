package com.interviewcoach.target.repository;

import com.interviewcoach.target.entity.InterviewTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewTargetRepository extends JpaRepository<InterviewTarget, UUID> {
    List<InterviewTarget> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<InterviewTarget> findByIdAndUserId(UUID id, UUID userId);
}
