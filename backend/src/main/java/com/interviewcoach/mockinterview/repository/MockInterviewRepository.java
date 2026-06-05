package com.interviewcoach.mockinterview.repository;

import com.interviewcoach.mockinterview.entity.MockInterview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 模拟面试会话数据访问接口。
 */
public interface MockInterviewRepository extends JpaRepository<MockInterview, UUID> {
    /** 按 ID 和用户 ID 查找面试会话，确保用户归属 */
    Optional<MockInterview> findByIdAndUserId(UUID id, UUID userId);
    /** 按目标岗位和用户查询面试列表，按创建时间倒序 */
    List<MockInterview> findByTargetIdAndUserIdOrderByCreatedAtDesc(UUID targetId, UUID userId);
    /** 删除指定目标岗位下的所有面试 */
    void deleteByTargetId(UUID targetId);
    /** 删除指定用户的所有面试 */
    void deleteByUserId(UUID userId);
}
