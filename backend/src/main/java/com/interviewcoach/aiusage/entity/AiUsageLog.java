package com.interviewcoach.aiusage.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 请求级 AI token 使用账本。只追加低风险元数据，不保存 prompt、completion 或用户原文。
 */
@Entity
@Table(name = "ai_usage_logs", indexes = {
        @Index(name = "idx_ai_usage_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_ai_usage_user_task_created", columnList = "user_id, task, created_at"),
        @Index(name = "idx_ai_usage_user_model_created", columnList = "user_id, model, created_at"),
        @Index(name = "idx_ai_usage_target_created", columnList = "target_id, created_at"),
        @Index(name = "idx_ai_usage_request_id", columnList = "request_id")
})
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(nullable = false, length = 80)
    private String task;

    @Column(name = "provider_type", nullable = false, length = 40)
    private String providerType;

    @Column(name = "provider_id")
    private UUID providerId;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(nullable = false, length = 40)
    private String mode;

    @Column(name = "usage_source", nullable = false, length = 40)
    private String usageSource;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "cache_creation_tokens", nullable = false)
    private int cacheCreationTokens;

    @Column(name = "cache_read_tokens", nullable = false)
    private int cacheReadTokens;

    @Column(name = "reasoning_tokens", nullable = false)
    private int reasoningTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(nullable = false)
    private boolean estimated;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "parse_failed", nullable = false)
    private boolean parseFailed;

    @Column(name = "validation_failed", nullable = false)
    private boolean validationFailed;

    @Column(nullable = false)
    private boolean timeout;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }

    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }

    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getUsageSource() { return usageSource; }
    public void setUsageSource(String usageSource) { this.usageSource = usageSource; }

    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }

    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }

    public int getCacheCreationTokens() { return cacheCreationTokens; }
    public void setCacheCreationTokens(int cacheCreationTokens) { this.cacheCreationTokens = cacheCreationTokens; }

    public int getCacheReadTokens() { return cacheReadTokens; }
    public void setCacheReadTokens(int cacheReadTokens) { this.cacheReadTokens = cacheReadTokens; }

    public int getReasoningTokens() { return reasoningTokens; }
    public void setReasoningTokens(int reasoningTokens) { this.reasoningTokens = reasoningTokens; }

    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

    public boolean isEstimated() { return estimated; }
    public void setEstimated(boolean estimated) { this.estimated = estimated; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isParseFailed() { return parseFailed; }
    public void setParseFailed(boolean parseFailed) { this.parseFailed = parseFailed; }

    public boolean isValidationFailed() { return validationFailed; }
    public void setValidationFailed(boolean validationFailed) { this.validationFailed = validationFailed; }

    public boolean isTimeout() { return timeout; }
    public void setTimeout(boolean timeout) { this.timeout = timeout; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
