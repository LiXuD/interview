package com.interviewcoach.assessment.repository;

import com.interviewcoach.assessment.entity.AssessmentResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 测评结果数据访问接口。
 */
public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, UUID> {
    /** 按目标岗位和用户查询测评结果，按创建时间倒序 */
    List<AssessmentResult> findBySessionTargetIdAndSessionUserIdOrderByCreatedAtDesc(UUID targetId, UUID userId);
    /** 删除指定目标岗位下的所有测评结果 */
    void deleteBySessionTargetId(UUID targetId);
    /** 删除指定用户的所有测评结果 */
    void deleteBySessionUserId(UUID userId);
}
