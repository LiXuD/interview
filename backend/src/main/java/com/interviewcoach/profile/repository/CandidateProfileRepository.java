package com.interviewcoach.profile.repository;

import com.interviewcoach.profile.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 候选人简历摘要数据访问接口。
 */
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    /**
     * 按目标岗位和用户查询简历摘要。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @return 简历摘要实体，不存在时为空
     */
    Optional<CandidateProfile> findByTargetIdAndUserId(UUID targetId, UUID userId);

    /**
     * 按用户查询所有简历摘要，按确认时间倒序。
     *
     * @param userId 用户 ID
     * @return 简历摘要列表
     */
    List<CandidateProfile> findByUserIdOrderByConfirmedAtDesc(UUID userId);

    /**
     * 按目标岗位删除所有简历摘要。
     *
     * @param targetId 目标岗位 ID
     */
    void deleteByTargetId(UUID targetId);

    /**
     * 按用户删除所有简历摘要。
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(UUID userId);
}
