package com.interviewcoach.report.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 报告实体，统一存储测评报告和模拟面试复盘报告。
 * type 字段区分来源：assessment 或 mockInterview。
 */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 报告类型：assessment（测评报告）或 mockInterview（模拟面试报告）。 */
    @Column(nullable = false)
    private String type;

    /** 报告内容，存储序列化的 JSON 字符串。 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) { createdAt = Instant.now(); }
    }

    public UUID getId() { return id; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getCreatedAt() { return createdAt; }
}
