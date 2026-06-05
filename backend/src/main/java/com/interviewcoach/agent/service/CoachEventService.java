package com.interviewcoach.agent.service;

import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.entity.CoachEventRecord;
import com.interviewcoach.agent.entity.InterviewCoachAgent;
import com.interviewcoach.agent.repository.AgentRepository;
import com.interviewcoach.agent.repository.CoachEventRepository;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 教练事件业务服务。负责事件的记录（含幂等去重）、认领、完成标记和失败标记。
 * <p>事件记录后通过 {@link ApplicationEventPublisher} 发布 {@link CoachEventRecorded}，
 * 触发下游 Agent 异步处理。</p>
 */
@Service
public class CoachEventService {

    private final CoachEventRepository eventRepository;
    private final AgentRepository agentRepository;
    private final InterviewTargetRepository targetRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CoachEventService(CoachEventRepository eventRepository,
                             AgentRepository agentRepository,
                             InterviewTargetRepository targetRepository,
                             UserRepository userRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.agentRepository = agentRepository;
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 记录教练事件（使用默认幂等键：eventType:sourceType:sourceId）。
     * 若已存在相同幂等键的事件则直接返回已有记录。
     *
     * @param user       当前用户实体
     * @param targetId   目标岗位 ID
     * @param eventType  事件类型
     * @param sourceType 事件来源实体类型（如 assessment、mockInterview）
     * @param sourceId   事件来源实体 ID
     * @return 事件记录（新建或已存在）
     * @throws com.interviewcoach.common.error.TargetNotFoundException 目标不存在或不属于当前用户
     * @throws IllegalStateException                                    教练 Agent 不存在
     */
    @Transactional
    public CoachEventRecord recordEvent(User user,
                                        UUID targetId,
                                        CoachEvent eventType,
                                        String sourceType,
                                        UUID sourceId) {
        String discriminator = eventType.name() + ":" + sourceType + ":" + sourceId;
        return recordEvent(user, targetId, eventType, sourceType, sourceId, discriminator);
    }

    /**
     * 按用户 ID 记录事件，自动查找 User 实体。
     *
     * @param userId     用户 ID
     * @param targetId   目标岗位 ID
     * @param eventType  事件类型
     * @param sourceType 事件来源实体类型
     * @param sourceId   事件来源实体 ID
     * @return 事件记录（新建或已存在）
     * @throws IllegalArgumentException 用户不存在
     */
    @Transactional
    public CoachEventRecord recordEvent(UUID userId,
                                        UUID targetId,
                                        CoachEvent eventType,
                                        String sourceType,
                                        UUID sourceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return recordEvent(user, targetId, eventType, sourceType, sourceId);
    }

    /**
     * 按用户 ID 和自定义幂等判别符记录事件。
     *
     * @param userId                    用户 ID
     * @param targetId                  目标岗位 ID
     * @param eventType                 事件类型
     * @param sourceType                事件来源实体类型
     * @param sourceId                  事件来源实体 ID
     * @param idempotencyDiscriminator 自定义幂等判别符（SHA-256 前的原始字符串）
     * @return 事件记录（新建或已存在）
     * @throws IllegalArgumentException 用户不存在
     */
    @Transactional
    public CoachEventRecord recordEvent(UUID userId,
                                        UUID targetId,
                                        CoachEvent eventType,
                                        String sourceType,
                                        UUID sourceId,
                                        String idempotencyDiscriminator) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return recordEvent(user, targetId, eventType, sourceType, sourceId, idempotencyDiscriminator);
    }

    /**
     * 记录教练事件的核心方法。校验目标归属、查找 Agent、生成幂等键并持久化事件。
     * 事件保存后发布 {@link CoachEventRecorded} 应用事件。
     *
     * @param user                      当前用户实体
     * @param targetId                  目标岗位 ID
     * @param eventType                 事件类型
     * @param sourceType                事件来源实体类型
     * @param sourceId                  事件来源实体 ID
     * @param idempotencyDiscriminator 自定义幂等判别符（SHA-256 前的原始字符串）
     * @return 事件记录（新建或已存在）
     * @throws com.interviewcoach.common.error.TargetNotFoundException 目标不存在或不属于当前用户
     * @throws IllegalStateException                                    教练 Agent 不存在
     */
    @Transactional
    public CoachEventRecord recordEvent(User user,
                                        UUID targetId,
                                        CoachEvent eventType,
                                        String sourceType,
                                        UUID sourceId,
                                        String idempotencyDiscriminator) {
        // 1. 校验目标归属
        targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));

        // 2. 查找该目标岗位对应的教练 Agent
        InterviewCoachAgent agent = agentRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new IllegalStateException("Coach agent not found for target: " + targetId));

        // 3. 计算幂等键，若已存在则直接返回；否则持久化并发布应用事件
        String idempotencyKey = sha256(idempotencyDiscriminator);
        return eventRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    CoachEventRecord event = eventRepository.save(newEvent(
                            agent, user.getId(), targetId, eventType, sourceType, sourceId, idempotencyKey));
                    eventPublisher.publishEvent(new CoachEventRecorded(event.getId()));
                    return event;
                });
    }

    /**
     * 认领事件用于处理。将 pending/failed 状态转为 processing，
     * 使用 REQUIRES_NEW 事务确保独立提交。
     *
     * @param eventId 教练事件记录 ID
     * @return 认领成功返回事件记录，已被其他线程认领返回 null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CoachEventRecord claim(UUID eventId) {
        int updated = eventRepository.claimForProcessing(eventId);
        if (updated == 0) {
            return null;
        }
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Coach event not found after claim: " + eventId));
    }

    /**
     * 标记事件处理完成。使用独立事务提交。
     *
     * @param eventId 教练事件记录 ID
     * @throws IllegalStateException 事件不存在
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID eventId) {
        CoachEventRecord event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Coach event not found: " + eventId));
        event.setStatus("completed");
        event.setLastErrorType(null);
        event.setProcessedAt(java.time.Instant.now());
    }

    /**
     * 标记事件处理失败。使用独立事务提交。
     *
     * @param eventId   教练事件记录 ID
     * @param errorType 异常类型名（用于诊断）
     * @throws IllegalStateException 事件不存在
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID eventId, String errorType) {
        CoachEventRecord event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Coach event not found: " + eventId));
        event.setStatus("failed");
        event.setLastErrorType(errorType);
        event.setProcessedAt(java.time.Instant.now());
    }

    private CoachEventRecord newEvent(InterviewCoachAgent agent,
                                      UUID userId,
                                      UUID targetId,
                                      CoachEvent eventType,
                                      String sourceType,
                                      UUID sourceId,
                                      String idempotencyKey) {
        CoachEventRecord event = new CoachEventRecord();
        event.setAgent(agent);
        event.setUserId(userId);
        event.setTargetId(targetId);
        event.setEventType(eventType.name());
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        event.setIdempotencyKey(idempotencyKey);
        return event;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
