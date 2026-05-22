package com.interviewcoach.report.service;

import com.interviewcoach.common.api.ReportDto;
import com.interviewcoach.common.error.ReportNotFoundException;
import com.interviewcoach.report.entity.Report;
import com.interviewcoach.report.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public ReportDto getReport(UUID reportId, UUID userId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
        return toDto(report);
    }

    @Transactional(readOnly = true)
    public List<ReportDto> listReports(UUID targetId, UUID userId) {
        return reportRepository.findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ReportDto toDto(Report report) {
        return new ReportDto(
                report.getId().toString(),
                report.getTargetId().toString(),
                report.getType(),
                report.getContent(),
                report.getCreatedAt().toString()
        );
    }
}
