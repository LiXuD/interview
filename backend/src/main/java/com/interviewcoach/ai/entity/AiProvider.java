package com.interviewcoach.ai.entity;

import com.interviewcoach.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 用户自定义 AI Provider 实体。存储 OpenAI-compatible 兼容的 Provider 配置，
 * 包含加密后的 API Key、base URL、模型名称和 API 模式。
 */
@Entity
@Table(name = "ai_providers")
public class AiProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 用户自定义的 Provider 显示名称 */
    @Column(nullable = false)
    private String name;

    /** OpenAI-compatible API 的 base URL */
    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    /** AES-GCM 加密后的 API Key 密文 */
    @Column(name = "api_key_encrypted", nullable = false)
    private String apiKeyEncrypted;

    /** 模型名称，如 gpt-4o */
    @Column(nullable = false)
    private String model;

    /** API 模式：chatCompletions 或 responses */
    @Column(name = "openai_api_mode", nullable = false)
    private String openaiApiMode;

    /** 是否为当前用户的默认 Provider */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKeyEncrypted() { return apiKeyEncrypted; }
    public void setApiKeyEncrypted(String apiKeyEncrypted) { this.apiKeyEncrypted = apiKeyEncrypted; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getOpenaiApiMode() { return openaiApiMode; }
    public void setOpenaiApiMode(String openaiApiMode) { this.openaiApiMode = openaiApiMode; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public Instant getCreatedAt() { return createdAt; }
}
