package com.interviewcoach.aiusage.repository;

import com.interviewcoach.aiusage.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {

    List<AiUsageLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID userId, Instant startAt, Instant endAt);
}
