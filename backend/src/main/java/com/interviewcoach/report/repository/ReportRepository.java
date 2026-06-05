package com.interviewcoach.report.repository;

import com.interviewcoach.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 报告数据访问接口。
 */
public interface ReportRepository extends JpaRepository<Report, UUID> {
    /**
     * 按 ID 和用户查询报告，确保用户归属。
     *
     * @param id     报告 ID
     * @param userId 用户 ID
     * @return 报告实体，不存在时为空
     */
    Optional<Report> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 按目标岗位和用户查询报告列表，按创建时间倒序。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @return 报告列表
     */
    List<Report> findByTargetIdAndUserIdOrderByCreatedAtDesc(UUID targetId, UUID userId);

    /**
     * 按目标岗位删除所有报告。
     *
     * @param targetId 目标岗位 ID
     */
    void deleteByTargetId(UUID targetId);

    /**
     * 按用户删除所有报告。
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(UUID userId);
}
