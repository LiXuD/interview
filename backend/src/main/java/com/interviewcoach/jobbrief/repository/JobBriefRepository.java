package com.interviewcoach.jobbrief.repository;

import com.interviewcoach.jobbrief.entity.JobBrief;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobBriefRepository extends JpaRepository<JobBrief, UUID> {
    Optional<JobBrief> findByTargetIdAndUserId(UUID targetId, UUID userId);
    void deleteByTargetId(UUID targetId);
    void deleteByUserId(UUID userId);
}
