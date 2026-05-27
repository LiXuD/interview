package com.interviewcoach.coachingmemory.repository;

import com.interviewcoach.coachingmemory.entity.CoachingMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoachingMemoryRepository extends JpaRepository<CoachingMemory, UUID> {
    List<CoachingMemory> findByTargetIdAndUserIdOrderByCreatedAtDesc(UUID targetId, UUID userId);
    Optional<CoachingMemory> findByIdAndUserId(UUID id, UUID userId);
    void deleteByUserId(UUID userId);
    void deleteByTargetId(UUID targetId);
}
