package com.interviewcoach.mockinterview.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 模拟面试消息实体，记录面试过程中每一轮的对话内容。
 */
@Entity
@Table(name = "mock_interview_messages")
public class MockInterviewMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private MockInterview interview;

    /** 消息角色：user 或 assistant */
    @Column(nullable = false)
    private String role;

    /** 消息内容 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) { createdAt = Instant.now(); }
    }

    public UUID getId() { return id; }

    public MockInterview getInterview() { return interview; }
    public void setInterview(MockInterview interview) { this.interview = interview; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getCreatedAt() { return createdAt; }
}
