package com.interviewcoach.agent.repository;

import com.interviewcoach.agent.entity.CoachEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 教练事件记录数据访问接口。支持幂等查询、待处理事件捞取、
 * 事件认领（claim）和按目标/用户批量删除。
 */
public interface CoachEventRepository extends JpaRepository<CoachEventRecord, UUID> {

    /** 按幂等键查询事件，用于去重 */
    Optional<CoachEventRecord> findByIdempotencyKey(String idempotencyKey);

    /** 捞取待处理或失败且未超重试上限的事件，按创建时间正序 */
    List<CoachEventRecord> findTop20ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            Collection<String> statuses, int maxAttempts);

    /** 认领事件：将 pending/failed 状态转为 processing 并递增尝试次数，返回受影响行数 */
    @Modifying
    @Query("""
            update CoachEventRecord e
               set e.status = 'processing',
                   e.attemptCount = e.attemptCount + 1
             where e.id = :id
               and e.status in ('pending', 'failed')
            """)
    int claimForProcessing(@Param("id") UUID id);

    /** 按目标岗位 ID 删除所有事件 */
    void deleteByTargetId(UUID targetId);

    /** 按用户 ID 删除所有事件 */
    void deleteByUserId(UUID userId);
}
