package com.interviewcoach.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * 教练事件记录实体。持久化每个触发教练 Agent 的业务事件，
 * 支持幂等去重（基于 idempotency_key）、状态流转和重试计数。
 */
@Entity
@Table(
        name = "coach_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coach_event_idempotency_key",
                columnNames = "idempotency_key"))
public class CoachEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private InterviewCoachAgent agent;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    /** 事件类型，对应 {@link CoachEvent} 枚举名 */
    @Column(nullable = false)
    private String eventType;

    /** 事件来源实体类型（如 assessment、mockInterview） */
    @Column(nullable = false)
    private String sourceType;

    /** 事件来源实体 ID */
    @Column(nullable = false)
    private UUID sourceId;

    /** 幂等键，SHA-256(eventType:sourceType:sourceId)，防止重复处理 */
    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    /** 事件状态：pending / processing / completed / failed */
    @Column(nullable = false)
    private String status = "pending";

    /** 已尝试处理次数 */
    @Column(nullable = false)
    private int attemptCount;

    /** 最近一次失败的异常类型名 */
    @Column
    private String lastErrorType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** 事件处理完成时间 */
    @Column
    private Instant processedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public InterviewCoachAgent getAgent() {
        return agent;
    }

    public void setAgent(InterviewCoachAgent agent) {
        this.agent = agent;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getLastErrorType() {
        return lastErrorType;
    }

    public void setLastErrorType(String lastErrorType) {
        this.lastErrorType = lastErrorType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
