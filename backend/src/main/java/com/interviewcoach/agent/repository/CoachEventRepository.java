package com.interviewcoach.agent.repository;

import com.interviewcoach.agent.entity.CoachEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoachEventRepository extends JpaRepository<CoachEventRecord, UUID> {

    Optional<CoachEventRecord> findByIdempotencyKey(String idempotencyKey);

    List<CoachEventRecord> findTop20ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            Collection<String> statuses, int maxAttempts);

    @Modifying
    @Query("""
            update CoachEventRecord e
               set e.status = 'processing',
                   e.attemptCount = e.attemptCount + 1
             where e.id = :id
               and e.status in ('pending', 'failed')
            """)
    int claimForProcessing(@Param("id") UUID id);

    void deleteByTargetId(UUID targetId);

    void deleteByUserId(UUID userId);
}
