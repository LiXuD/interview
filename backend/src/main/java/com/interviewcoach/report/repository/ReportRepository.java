package com.interviewcoach.report.repository;

import com.interviewcoach.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    Optional<Report> findByIdAndUserId(UUID id, UUID userId);
    List<Report> findByTargetIdAndUserIdOrderByCreatedAtDesc(UUID targetId, UUID userId);
    void deleteByTargetId(UUID targetId);
    void deleteByUserId(UUID userId);
}
