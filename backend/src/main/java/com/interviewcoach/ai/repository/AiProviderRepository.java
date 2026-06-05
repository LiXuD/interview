package com.interviewcoach.ai.repository;

import com.interviewcoach.ai.entity.AiProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI Provider 数据访问接口。按用户 ID 查询、删除 Provider，以及查找默认 Provider。
 */
public interface AiProviderRepository extends JpaRepository<AiProvider, UUID> {

    /**
     * 按用户 ID 查询所有 Provider，按创建时间倒序
     *
     * @param userId 用户 ID
     * @return Provider 列表
     */
    List<AiProvider> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 按 Provider ID 和用户 ID 查询，确保归属校验
     *
     * @param id     Provider ID
     * @param userId 用户 ID
     * @return Provider 实体（可选）
     */
    Optional<AiProvider> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 查找用户的默认 Provider
     *
     * @param userId 用户 ID
     * @return 默认 Provider 实体（可选）
     */
    Optional<AiProvider> findByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * 判断用户是否已有任何 Provider
     *
     * @param userId 用户 ID
     * @return 如果存在则返回 true
     */
    boolean existsByUserId(UUID userId);

    /**
     * 删除用户的所有 Provider
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(UUID userId);
}
