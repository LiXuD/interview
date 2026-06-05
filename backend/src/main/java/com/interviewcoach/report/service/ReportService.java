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

/**
 * 报告服务，提供报告查询和 DTO 转换。
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * 获取指定报告详情。
     *
     * @param reportId 报告 ID
     * @param userId   用户 ID
     * @return 报告 DTO
     * @throws ReportNotFoundException 报告不存在或不属于该用户
     */
    @Transactional(readOnly = true)
    public ReportDto getReport(UUID reportId, UUID userId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
        return toDto(report);
    }

    /**
     * 获取指定目标岗位下所有报告。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @return 报告 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<ReportDto> listReports(UUID targetId, UUID userId) {
        return reportRepository.findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 将报告实体转换为 DTO。
     *
     * @param report 报告实体
     * @return 报告 DTO
     */
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
