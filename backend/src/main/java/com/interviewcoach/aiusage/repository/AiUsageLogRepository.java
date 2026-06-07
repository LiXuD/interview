package com.interviewcoach.aiusage.repository;

import com.interviewcoach.aiusage.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {

    List<AiUsageLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID userId, Instant startAt, Instant endAt);

    /** 查询指定时间范围内的所有用量日志（管理端跨用户聚合） */
    List<AiUsageLog> findByCreatedAtBetween(Instant startAt, Instant endAt);

    /** 查询指定用户在指定时间范围内的用量日志（管理端单用户详情） */
    List<AiUsageLog> findByUserIdInAndCreatedAtBetween(List<UUID> userIds, Instant startAt, Instant endAt);

    /** 按 providerType 汇总单用户在时间范围内的 totalTokens（避免加载全部日志到内存） */
    @Query("SELECT COALESCE(SUM(l.totalTokens), 0) FROM AiUsageLog l " +
           "WHERE l.userId = :userId AND l.providerType = :providerType " +
           "AND l.createdAt BETWEEN :startAt AND :endAt")
    long sumTotalTokensByUserIdAndProviderTypeAndCreatedAtBetween(
            @Param("userId") UUID userId,
            @Param("providerType") String providerType,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt);

    /** 按 providerType 汇总多个用户在时间范围内的 totalTokens，返回 [userId, sum] 对 */
    @Query("SELECT l.userId, SUM(l.totalTokens) FROM AiUsageLog l " +
           "WHERE l.userId IN :userIds AND l.providerType = :providerType " +
           "AND l.createdAt BETWEEN :startAt AND :endAt GROUP BY l.userId")
    List<Object[]> sumTotalTokensByUserIdsAndProviderTypeAndCreatedAtBetween(
            @Param("userIds") List<UUID> userIds,
            @Param("providerType") String providerType,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt);
}
