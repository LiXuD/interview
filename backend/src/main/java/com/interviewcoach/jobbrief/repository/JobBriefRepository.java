package com.interviewcoach.jobbrief.repository;

import com.interviewcoach.jobbrief.entity.JobBrief;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 岗位画像数据访问接口。
 */
public interface JobBriefRepository extends JpaRepository<JobBrief, UUID> {
    /** 按目标岗位和用户查询岗位画像 */
    Optional<JobBrief> findByTargetIdAndUserId(UUID targetId, UUID userId);
    /** 删除指定目标岗位下的所有岗位画像 */
    void deleteByTargetId(UUID targetId);
    /** 删除指定用户的所有岗位画像 */
    void deleteByUserId(UUID userId);
}
