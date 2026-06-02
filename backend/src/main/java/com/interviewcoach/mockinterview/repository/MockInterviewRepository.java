package com.interviewcoach.mockinterview.repository;

import com.interviewcoach.mockinterview.entity.MockInterview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockInterviewRepository extends JpaRepository<MockInterview, UUID> {
    Optional<MockInterview> findByIdAndUserId(UUID id, UUID userId);
    List<MockInterview> findByTargetIdAndUserIdOrderByCreatedAtDesc(UUID targetId, UUID userId);
    void deleteByTargetId(UUID targetId);
    void deleteByUserId(UUID userId);
}
