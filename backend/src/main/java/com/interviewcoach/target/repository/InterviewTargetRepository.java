package com.interviewcoach.target.repository;

import com.interviewcoach.target.entity.InterviewTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 目标岗位数据访问接口。
 */
public interface InterviewTargetRepository extends JpaRepository<InterviewTarget, UUID> {
    /**
     * 按用户查询所有目标岗位，按创建时间倒序。
     *
     * @param userId 用户 ID
     * @return 目标岗位列表
     */
    List<InterviewTarget> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 按 ID 和用户查询目标岗位，确保用户归属。
     *
     * @param id     目标岗位 ID
     * @param userId 用户 ID
     * @return 目标岗位实体，不存在时为空
     */
    Optional<InterviewTarget> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 按用户删除所有目标岗位。
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(UUID userId);
}
