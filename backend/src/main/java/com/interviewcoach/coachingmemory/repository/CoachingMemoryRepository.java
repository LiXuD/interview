package com.interviewcoach.coachingmemory.repository;

import com.interviewcoach.coachingmemory.entity.CoachingMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 教练记忆数据访问接口。
 */
public interface CoachingMemoryRepository extends JpaRepository<CoachingMemory, UUID> {
    /** 按目标岗位和用户查询教练记忆，按创建时间倒序 */
    List<CoachingMemory> findByTargetIdAndUserIdOrderByCreatedAtDesc(UUID targetId, UUID userId);
    /** 按记忆 ID 和用户查询，确保用户只能访问自己的记忆 */
    Optional<CoachingMemory> findByIdAndUserId(UUID id, UUID userId);
    /** 删除指定用户的所有教练记忆 */
    void deleteByUserId(UUID userId);
    /** 删除指定目标岗位下的所有教练记忆 */
    void deleteByTargetId(UUID targetId);
}
